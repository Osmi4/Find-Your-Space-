import React from 'react';
import { render, screen, waitFor, fireEvent, within } from '@testing-library/react';
import { useAuth0 } from '@auth0/auth0-react';
import axios from 'axios';
import MessagePage from '../pages/MessagePage';
import '@testing-library/jest-dom';
import { BrowserRouter as Router } from 'react-router-dom';

// Mock the necessary modules
jest.mock('@auth0/auth0-react');
jest.mock('axios');

describe('MessagePage component', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

test('displays error message when an error occurs while fetching messages', async () => {
    // Mock useAuth0 hook
    useAuth0.mockReturnValue({
      isAuthenticated: true,
      loginWithRedirect: jest.fn(),
      getIdTokenClaims: jest.fn()
    });

    // Mock axios to reject the fetch messages request
    axios.post.mockRejectedValue(new Error('Failed to fetch messages'));

    render(
        <Router>
            <MessagePage />
        </Router>
    );

    // Wait for the error message to be displayed
    const errorMessage = await waitFor(() => screen.getByText("An error occurred while trying to load messages. Please try again later."));

    expect(errorMessage).toBeInTheDocument();
  });
});