package com.example.backend.repository;

import com.example.backend.entity.Space;
import com.example.backend.enums.Availibility;
import com.example.backend.enums.SpaceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface SpaceRepository extends JpaRepository<Space, String> {
    Page<Space> findByOwner_UserId(String userId, Pageable pageable);

    Optional<Space> findBySpaceId(String spaceId);

    @Query("""
        SELECT s FROM Space s
        WHERE s.spacePrice BETWEEN :spacePriceLow AND :spacePriceUp
        AND s.spaceSize BETWEEN :spaceSizeLow AND :spaceSizeUp
        AND (:userId IS NULL OR s.owner.userId = :userId)
        AND (:spaceName IS NULL OR s.spaceName LIKE %:spaceName%)
        AND (:spaceLocation IS NULL OR s.spaceLocation LIKE %:spaceLocation%)
        AND (:spaceType IS NULL OR s.spaceType IN :spaceType)
        AND (:availability IS NULL OR s.availability IN :availability)
        """)
    Page<Space> findSpacesByFilters(
            @Param("spacePriceUp") double spacePriceUp,
            @Param("spacePriceLow") double spacePriceLow,
            @Param("spaceSizeUp") double spaceSizeUp,
            @Param("spaceSizeLow") double spaceSizeLow,
            @Nullable @Param("userId") String userId,
            @Nullable @Param("spaceName") String spaceName,
            @Nullable @Param("spaceLocation") String spaceLocation,
            @Nullable @Param("spaceType") List<SpaceType> spaceType,
            @Nullable @Param("availability") List<Availibility> availability,
            Pageable pageable);

    @Transactional
    @Modifying
    @Query("delete from Space s where s.spaceId = ?1")
    int deleteBySpaceId(String spaceId);

    @Query("select s from Space s inner join s.bookings bookings where bookings.bookingId = ?1")
    Optional<Space> findByBookings_BookingId(String bookingId);
    @Transactional
    @Modifying
    @Query("UPDATE Space s SET " +
            "s.spaceName = COALESCE(:spaceName, s.spaceName), " +
            "s.spaceLocation = COALESCE(:spaceLocation, s.spaceLocation), " +
            "s.spaceSize = CASE WHEN :spaceSize IS NULL THEN s.spaceSize ELSE :spaceSize END, " +
            "s.spacePrice = CASE WHEN :spacePrice IS NULL THEN s.spacePrice ELSE :spacePrice END, " +
            "s.spaceDescription = COALESCE(:spaceDescription, s.spaceDescription) " +
            "WHERE s.spaceId = :id")
    int updateSpace(@Param("id") String id,
                    @Param("spaceName") String spaceName,
                    @Param("spaceLocation") String spaceLocation,
                    @Param("spaceSize") Double spaceSize,
                    @Param("spacePrice") Double spacePrice,
                    @Param("spaceDescription") String spaceDescription);

    @Transactional
    @Modifying
    @Query("UPDATE Space s SET " +
            "s.availability = :availability " +
            "WHERE s.spaceId = :id")
    int updateSpaceAvailability(@Param("id") String id,@Param("availability") Availibility availability);



}
