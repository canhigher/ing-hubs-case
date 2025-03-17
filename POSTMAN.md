# ING Brokerage API - Postman Collection Guide

This document provides instructions on how to use the Postman collection for testing the ING Brokerage API.

## Table of Contents

- [Overview](#overview)
- [Setup](#setup)
- [Environment Variables](#environment-variables)
- [Authentication Flow](#authentication-flow)
- [Available Endpoints](#available-endpoints)
- [Testing Scenarios](#testing-scenarios)

## Overview

The ING Brokerage API Postman collection contains a comprehensive set of requests to test all endpoints of the brokerage API, including authentication, asset management, and order operations. The collection is organized into folders based on the functionality being tested.

## Setup

1. **Import the Collection**

   - Open Postman
   - Click on "Import" button in the top left
   - Select the `ING-Brokerage-API.postman_collection.json` file
   - The collection will be imported into your Postman workspace

2. **Create Environment**
   - Click on the "Environments" tab in Postman
   - Create a new environment (e.g., "ING Brokerage Local")
   - Add the necessary environment variables (see next section)
   - Save the environment
   - Select the environment from the dropdown in the top right corner

## Environment Variables

The collection uses the following environment variables:

| Variable          | Description                           | Initial Value                         |
| ----------------- | ------------------------------------- | ------------------------------------- |
| `base_url`        | Base URL of the API                   | `http://localhost:8080`               |
| `jwt_token`       | JWT token for customer authentication | (Automatically set after login)       |
| `admin_jwt_token` | JWT token for admin authentication    | (Automatically set after admin login) |
| `user_id`         | ID of the logged-in customer          | (Automatically set after login)       |
| `admin_id`        | ID of the logged-in admin             | (Automatically set after admin login) |

Only `base_url` needs to be manually set. The other variables are automatically populated when you run the login requests.

## Authentication Flow

The collection includes automatic token handling:

1. **Register Users**

   - Use the "Register User" request to create a customer account
   - Use the "Register Admin" request to create an admin account

2. **Login**

   - Use the "Login" request to authenticate as a customer
   - Use the "Login as Admin" request to authenticate as an admin
   - These requests automatically extract the JWT tokens and store them in environment variables

3. **Access Protected Endpoints**
   - Once logged in, you can access protected endpoints
   - The collection automatically includes the appropriate token in requests

## Available Endpoints

### Authentication

- **Register User**: Creates a new customer user
- **Register Admin**: Creates a new admin user
- **Login**: Authenticates a customer user
- **Login as Admin**: Authenticates an admin user

### Assets

- **Get Customer Assets**: Retrieves asset balances for a customer
- **Add Asset Balance (Admin)**: Adds balance to a customer's asset (admin only)

### Orders

- **Create Order**: Creates a new buy/sell order
- **Get Customer Orders**: Retrieves orders for a customer
- **Get All Orders (Admin)**: Retrieves all orders in the system (admin only)
- **Get Order by ID**: Retrieves a specific order
- **Cancel Order**: Cancels a pending order
- **Match Order (Admin)**: Matches a pending order (admin only)

## Testing Scenarios

### Basic Customer Flow

1. Register a customer user
2. Login as the customer
3. View the customer's assets
4. Create a buy order
5. Check the customer's orders
6. Cancel the order

### Admin Operations

1. Register an admin user
2. Login as the admin
3. Add balance to a customer's asset
4. View all orders in the system
5. Match a customer's pending order

### Error Handling

The collection includes example responses for various error scenarios:

- Trying to register with an existing username
- Attempting to access admin endpoints as a customer
- Trying to cancel someone else's order
- Requesting a non-existent order

## Notes

- The collection includes pre-request scripts and test scripts to automatically extract and store tokens
- Example responses are included for most requests to show expected format
- Some requests include optional query parameters that are disabled by default (e.g., filtering options for orders)
- Authentication tokens expire after a certain time (usually 24 hours) - if you encounter 401 errors, simply log in again
