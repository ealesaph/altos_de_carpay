import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';

import Home from './pages/Home.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import SelectDateSpace from './pages/SelectDateSpace.jsx';
import CheckoutReview from './pages/CheckoutReview.jsx';
import PaymentCard from './pages/PaymentCard.jsx';
import PaymentAuth from './pages/PaymentAuth.jsx';
import Confirmation from './pages/Confirmation.jsx';
import ClientReservations from './pages/ClientReservations.jsx';
import AdminDashboard from './pages/AdminDashboard.jsx';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Landing pública */}
        <Route path="/" element={<Home />} />

        {/* Autenticación */}
        <Route path="/login" element={<Login />} />
        <Route path="/registro" element={<Register />} />

        {/* Flujo de Reserva */}
        <Route path="/reservar/espacio" element={<SelectDateSpace />} />
        <Route path="/reservar/pago" element={<CheckoutReview />} />
        <Route path="/simulador/tarjeta" element={<PaymentCard />} />
        <Route path="/simulador/autorizacion" element={<PaymentAuth />} />
        <Route path="/reservar/confirmacion" element={<Confirmation />} />

        {/* Cliente: Mis Reservas */}
        <Route path="/mis-reservas" element={<ClientReservations />} />

        {/* Rol Administrativo */}
        <Route path="/admin" element={<AdminDashboard />} />
      </Routes>
    </BrowserRouter>
  );
}