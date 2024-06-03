import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import { Auth0Provider } from '@auth0/auth0-react';

import {NextUIProvider} from "@nextui-org/react";

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <Auth0Provider
            domain="dev-k72vb4107en0ey6q.us.auth0.com"
            clientId="a3smzZPJKNK3EHxrvjIH5OemY2LqvX8G"
            authorizationParams={{
                redirect_uri: window.location.origin,
                audience: "http://localhost:8080",
            }}
        >
    <NextUIProvider>
      <App />
    </NextUIProvider>
    </Auth0Provider>
  </React.StrictMode>
);
