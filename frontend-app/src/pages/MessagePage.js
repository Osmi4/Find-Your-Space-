import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useAuth0 } from '@auth0/auth0-react';
import { Button } from '@nextui-org/react';
import { useNavigate } from 'react-router-dom';
import { ClipLoader } from 'react-spinners';
import {ToastContainer} from "react-toastify";

const MessagesPage = () => {
    const { isAuthenticated } = useAuth0();
    const [messages, setMessages] = useState([]);
    const [uniqueSenders, setUniqueSenders] = useState([]);
    const [loading, setLoading] = useState(true);  // State to manage loading spinner
    const navigate = useNavigate();

    useEffect(() => {
        const fetchMessages = async () => {
            const token = localStorage.getItem('authToken');
            try {
                const response = await axios.get('http://localhost:8080/api/message/my-messages', {
                    headers: { Authorization: `Bearer ${token}` },
                    params: { page: 0, size: 100 } // Adjust the size as needed
                });

                const allMessages = response.data.content;
                const senderIds = [...new Set(allMessages.map(message => message.senderId))];
                setMessages(allMessages);

                const uniqueSendersPromises = senderIds.map(async (senderId) => {
                    const userResponse = await axios.get(`http://localhost:8080/api/user/${senderId}`, {
                        headers: { Authorization: `Bearer ${token}` }
                    });
                    return { ...userResponse.data, senderId };
                });

                const uniqueSenders = await Promise.all(uniqueSendersPromises);
                setUniqueSenders(uniqueSenders);
                setLoading(false);  // Set loading to false once data is fetched
            } catch (error) {
                console.error("Error fetching messages:", error);
                setLoading(false);  // Set loading to false even if there's an error
            }
        };

        if (isAuthenticated) {
            fetchMessages();
        }
    }, [isAuthenticated]);

    const handleBoxClick = (senderId) => {
        navigate(`/message/${senderId}`);
    };

    if (loading) {
        return (
            <div className="flex justify-center items-center h-screen">
                <ClipLoader size={35} color={"#123abc"} loading={loading} />
            </div>
        );
    }

    return (
        <div className="container mx-auto p-4">
            <ToastContainer />
            <h1 className="text-2xl font-bold mb-4">My Messages</h1>
            {uniqueSenders.length === 0 ? (
                <p>No messages</p>
            ) : (
                <div className="grid grid-cols-1 gap-4">
                    {uniqueSenders.map(sender => (
                        <div
                            key={sender.senderId}
                            className="bg-white p-4 rounded-lg shadow-md cursor-pointer"
                            onClick={() => handleBoxClick(sender.senderId)}
                        >
                            <p className="text-lg font-semibold">{sender.firstName} {sender.lastName}</p>
                            <p className="text-gray-600">{sender.email}</p>
                        </div>
                    ))}
                </div>
            )}
            {/*<Button onClick={() => navigate('/message/new')} className="mt-4">Send a New Message</Button>*/}
        </div>
    );
};

export default MessagesPage;
