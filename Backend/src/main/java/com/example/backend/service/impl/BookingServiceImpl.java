package com.example.backend.service.impl;

import com.example.backend.autoMapper.BookingMapper;
import com.example.backend.dtos.Booking.AddBookingRequest;
import com.example.backend.dtos.Booking.BookingFilter;
import com.example.backend.dtos.Booking.BookingResponse;
import com.example.backend.dtos.Booking.EditBookingRequest;
import com.example.backend.entity.Booking;
import com.example.backend.entity.Space;
import com.example.backend.entity.User;
import com.example.backend.enums.Availibility;
import com.example.backend.enums.PermissionType;
import com.example.backend.enums.Status;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.BookingRepository;
import com.example.backend.repository.SpaceRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.BookingService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.AccessDeniedException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final SpaceRepository spaceRepository;
    private final UserRepository userRepository;
    private final PermissionServiceImpl permissionService;

    public BookingServiceImpl(BookingRepository bookingRepository, SpaceRepository spaceRepository, UserRepository userRepository, PermissionServiceImpl permissionService) {
        this.bookingRepository = bookingRepository;
        this.spaceRepository = spaceRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
    }
    @Override
    public BookingResponse getBooking(String id) {
        Optional<Booking> bookingOpt = bookingRepository.findByBookingId(id);
        Booking booking = bookingOpt.orElse(null);
        if(booking == null) {
            throw new ResourceNotFoundException("Booking not found", "booking", id);
        }
        return BookingMapper.INSTANCE.bookingToBookingResponse(booking);
    }

    @Override
    public BookingResponse addBooking(AddBookingRequest addBookingRequest) {
        List<Booking> currentBookings = bookingRepository.findBySpace_SpaceId(addBookingRequest.getSpaceId());
        if (checkAvailability(currentBookings, addBookingRequest.getEndDateTime(), addBookingRequest.getStartDateTime())){
            throw new RuntimeException("Space is not available at this time");
        }
        Optional<Space> spaceOpt = spaceRepository.findBySpaceId(addBookingRequest.getSpaceId());
        Space space = spaceOpt.orElse(null);
        if(space == null) {
            throw new ResourceNotFoundException("Space not found", "space", addBookingRequest.getSpaceId());
        }
        if(!space.getAvailability().equals(Availibility.AVAILABLE)){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Space not available");
        }
        double days = (addBookingRequest.getEndDateTime().getTime() - addBookingRequest.getStartDateTime().getTime()) / 1000 / 60 / 60 / 24;
        double price = space.getSpacePrice() * days;
        //User client = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User client = userRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(
                () -> new ResourceNotFoundException("User not found!", "email", SecurityContextHolder.getContext().getAuthentication().getName()));
        User Owner = space.getOwner();
        if (Owner.getUserId().equals(client.getUserId())) {
            throw new IllegalArgumentException("Owner can't book their own space");
        }
        Booking addedBooking = bookingRepository.save(BookingMapper.INSTANCE.addBookingRequestToBooking(addBookingRequest, price, client, space));
        // add permissions

        permissionService.createPermissionFromListOfPermissions(addedBooking.getClient().getEmail(), Booking.class.getSimpleName(),addedBooking.getBookingId(), PermissionServiceImpl.OWNER_PERMISSIONS);
        permissionService.createPermissionsForAdminsFromListOfPermissions(addedBooking.getClient().getEmail(),Booking.class.getSimpleName(), addedBooking.getBookingId(), PermissionServiceImpl.ADMIN_PERMISSIONS);
        return BookingMapper.INSTANCE.bookingToBookingResponse(addedBooking);
    }



    @Override
    public BookingResponse updateBooking(EditBookingRequest editBookingRequest, String bookingId) throws AccessDeniedException {
        permissionService.checkPermission(SecurityContextHolder.getContext().getAuthentication().getName(), Booking.class.getSimpleName(), bookingId, PermissionType.UPDATE);

        Booking bookingToUpdate = bookingRepository.findByBookingId(bookingId).stream().findFirst().orElse(null);
        if(bookingToUpdate == null || !bookingToUpdate.getStatus().equals(Status.INQUIRY) || !bookingToUpdate.getClient().getUserId().equals(userRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(
                () -> new ResourceNotFoundException("User not found!", "email", SecurityContextHolder.getContext().getAuthentication().getName())).getUserId())){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "booking is not found or not owned by user or not in inquiry status");
        }
        if(editBookingRequest.getStartDate()==null){
            editBookingRequest.setStartDate(bookingToUpdate.getStartDateTime());
        }
        if(editBookingRequest.getEndDate()==null){
            editBookingRequest.setEndDate(bookingToUpdate.getEndDateTime());
        }
        List<Booking> currentBookings = bookingRepository.findBySpace_SpaceId(bookingToUpdate.getSpace().getSpaceId());
        currentBookings.remove(bookingToUpdate);
        if (checkAvailability(currentBookings, editBookingRequest.getEndDate(), editBookingRequest.getStartDate())){
            throw new RuntimeException("Space is not available at this time");
        }
        double price = bookingToUpdate.getSpace().getSpacePrice()* (editBookingRequest.getEndDate().getTime() - editBookingRequest.getStartDate().getTime()) / 1000 / 60 / 60;
        int affectedRows = bookingRepository.updateBookingDate(bookingId, editBookingRequest.getStartDate(), editBookingRequest.getEndDate(), price);
        if(affectedRows == 0){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "booking not updated");
        }
        Booking updatedBooking = bookingRepository.findByBookingId(bookingId).stream().findFirst().orElse(null);
        if(updatedBooking == null){
            throw new ResourceNotFoundException("Booking not found", "booking", bookingId);
        }
        return BookingMapper.INSTANCE.bookingToBookingResponse(updatedBooking);
    }

    private static boolean checkAvailability(List<Booking> currentBookings, Date editBookingRequest, Date editBookingRequest1) {
        for (Booking booking : currentBookings) {
            if (booking.getStartDateTime().before(editBookingRequest) && booking.getEndDateTime().after(editBookingRequest1)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public BookingResponse deleteBooking(String id) throws AccessDeniedException {
        permissionService.checkPermission(SecurityContextHolder.getContext().getAuthentication().getName(), Booking.class.getSimpleName(), id, PermissionType.DELETE);

        Booking bookingToDelete = bookingRepository.findByBookingId(id).stream().findFirst().orElse(null);
        if(bookingToDelete==null){
            throw new ResourceNotFoundException("Booking not found", "booking", id);
        }
        if(!bookingToDelete.getStatus().equals(Status.INQUIRY)){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "booking is not in inquiry status");
        }
        if(!bookingToDelete.getClient().getUserId().equals(userRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(
                () -> new ResourceNotFoundException("User not found!", "email", SecurityContextHolder.getContext().getAuthentication().getName())).getUserId())){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "booking is not owned by user");
        }
        int deleted =bookingRepository.deleteByBookingId(id);
        if(deleted == 0 || bookingRepository.findByBookingId(id).isPresent()){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "booking not deleted");
        }

        return BookingMapper.INSTANCE.bookingToBookingResponse(bookingToDelete);
    }

    @Override
    public Page<BookingResponse> getMyBookings(Pageable pageable) {
        //User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = (User)userRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(
                () -> new ResourceNotFoundException("User not found!", "email", SecurityContextHolder.getContext().getAuthentication().getName()));
        //do sprawdzenia
        if(user == null){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "user not log in");
        }
        else{
            Page<Booking> bookings = bookingRepository.findByClient_UserId(user.getUserId(), pageable);
            return bookings.map(BookingMapper.INSTANCE::bookingToBookingResponse);
        }
    }

    @Override
    public Page<BookingResponse> getSearchMyBookings(Optional<BookingFilter> filterOpt , Pageable pageable) {
        if(filterOpt.isEmpty()) {
            return getMyBookings(pageable);
        }
        else{
            BookingFilter filter = filterOpt.get();
            //User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User user = (User)userRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(
                    () -> new ResourceNotFoundException("User not found!", "email", SecurityContextHolder.getContext().getAuthentication().getName()));
            Page<Booking> bookings = doFilter(filter , Optional.of(user) , Optional.empty() , pageable);
            return bookings.map(BookingMapper.INSTANCE::bookingToBookingResponse);
        }
    }

    @Override
    public Page<BookingResponse> getBookingForSpace(String spaceId, Optional<BookingFilter> filter , Pageable pageable) {
        //User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = (User)userRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(
                () -> new ResourceNotFoundException("User not found!", "email", SecurityContextHolder.getContext().getAuthentication().getName()));
        if(user == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "user not log in");
        }
        Optional<Space> spaceOpt = spaceRepository.findBySpaceId(spaceId);
        Space space = spaceOpt.orElse(null);
        if(space == null || !space.getOwner().getUserId().equals(user.getUserId())) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "space not found or not owned by user");
        }
        if(filter.isEmpty()){
            return bookingRepository.findBySpace_SpaceId(spaceId, pageable).map(BookingMapper.INSTANCE::bookingToBookingResponse);
        }
        else{
            return doFilter(filter.get() , Optional.empty() , Optional.of(space) , pageable).map(BookingMapper.INSTANCE::bookingToBookingResponse);
        }
    }

    @Override
    public Page<BookingResponse> getSearchAllBookings(Optional<BookingFilter> filter , Pageable pageable) {
        if(filter.isEmpty()) {
            Page<Booking> bookings = bookingRepository.findAll(pageable);
            return bookings.map(BookingMapper.INSTANCE::bookingToBookingResponse);
        }
        else{
            Page<Booking> bookings = doFilter(filter.get() , Optional.empty() , Optional.empty() , pageable);
            return bookings.map(BookingMapper.INSTANCE::bookingToBookingResponse);
        }
    }

    @Override
    public BookingResponse updateBookingStatus(Status status, String id) throws AccessDeniedException {
        permissionService.checkPermission(SecurityContextHolder.getContext().getAuthentication().getName(), Booking.class.getSimpleName(), id, PermissionType.UPDATE);

        // User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(
                () -> new ResourceNotFoundException("User not found!", "email", SecurityContextHolder.getContext().getAuthentication().getName()));

        Space space = spaceRepository.findByBookings_BookingId(id).orElse(null);
        assert space != null;
        if(!user.getUserId().equals(space.getOwner().getUserId())){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "user not authorized to change status");
        }
        Booking bookingToUpdate = bookingRepository.findByBookingId(id).stream().findFirst().orElse(null);
        if(bookingToUpdate == null) {
            throw new ResourceNotFoundException("Booking not found", "booking", id);
        }
        boolean accept=false;
        switch(bookingToUpdate.getStatus()){
            case INQUIRY:
                if(status.equals(Status.ACCEPTED) || status.equals(Status.REJECTED)){
                    accept=true;
                }
            case ACCEPTED:
                if(status.equals(Status.COMPLETED) || status.equals(Status.CANCELLED)){
                    accept=true;
                }
            break;
        }
        if(accept){
            bookingToUpdate.setStatus(status);
            bookingToUpdate.setDateUpdated(new Date());
            Booking updatedBooking = bookingRepository.save(bookingToUpdate);
            return BookingMapper.INSTANCE.bookingToBookingResponse(updatedBooking);
        }
        else{
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid status change");
        }
    }

    @Override
    public Page<BookingResponse> getBookingsForSpace(String spaceId, Pageable pageable) {
        Space space = spaceRepository.findBySpaceId(spaceId).orElse(null);
        if(space == null) {
            throw new ResourceNotFoundException("Space not found", "space", spaceId);
        }
        Page<Booking> bookings = bookingRepository.findBySpace_SpaceId(spaceId, pageable);
        return bookings.map(BookingMapper.INSTANCE::bookingToBookingResponse);
    }

    @Override
    public Page<BookingResponse> getAllBookings(Pageable pageable) {
        Page<Booking> bookings = bookingRepository.findAll(pageable);
        return bookings.map(BookingMapper.INSTANCE::bookingToBookingResponse);
    }

    Page<Booking> doFilter(BookingFilter bookingFilter, Optional<User> userOpt, Optional<Space> spaceOpt , Pageable pageable) {
        String clientId;
        if(userOpt.isPresent()){
            User user = userOpt.get();
            clientId = user.getUserId();
        }
        else{
            if(bookingFilter.getClientId()!=null){
                clientId = bookingFilter.getClientId();
            }
            else{
                clientId = null;
            }
        }
        if(spaceOpt.isPresent()){
            Space space = spaceOpt.get();
            bookingFilter.setOwnerId(space.getOwner().getUserId());
            bookingFilter.setSpaceId(space.getSpaceId());
        }
        return bookingRepository.filterQuery(bookingFilter.getStartDate(), bookingFilter.getEndDate(), clientId, bookingFilter.getOwnerId(), bookingFilter.getSpaceId() , bookingFilter.getStatus() , pageable);
    }

}
