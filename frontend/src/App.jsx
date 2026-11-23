import React, { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';

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
import Account from './pages/Account.jsx';
import Privacidad from './pages/Privacidad.jsx';
import Condiciones from './pages/Condiciones.jsx';

// Los enlaces «/#seccion» de otras páginas llevan a la sección correcta de la portada.
function DesplazarAlAncla() {
  const { pathname, hash } = useLocation();
  useEffect(() => {
    if (!hash) {
      window.scrollTo(0, 0);
      return;
    }
    const id = hash.slice(1);
    const intentar = (restantes) => {
      const elemento = document.getElementById(id);
      if (elemento) elemento.scrollIntoView({ behavior: 'smooth' });
      else if (restantes > 0) setTimeout(() => intentar(restantes - 1), 100);
    };
    intentar(10);
  }, [pathname, hash]);
  return null;
}

export default function App() {
  return (
    <BrowserRouter>
      <DesplazarAlAncla />
      <Routes>
        {/* Landing pública */}
        <Route path="/" element={<Home />} />

        {/* Autenticación */}
        <Route path="/login" element={<Login />} />
        <Route path="/registro" element={<Register />} />

        {/* Flujo de Reserva */}
        <Route path="/reservar" element={<Navigate to="/reservar/espacio" replace />} />
        <Route path="/reservar/espacio" element={<SelectDateSpace />} />
        <Route path="/reservar/pago" element={<CheckoutReview />} />
        <Route path="/simulador/tarjeta" element={<PaymentCard />} />
        <Route path="/simulador/autorizacion" element={<PaymentAuth />} />
        <Route path="/reservar/confirmacion" element={<Confirmation />} />

        {/* Cliente: Mis Reservas y Cuenta */}
        <Route path="/mis-reservas" element={<ClientReservations />} />
        <Route path="/mi-cuenta" element={<Account />} />
        <Route path="/privacidad" element={<Privacidad />} />
        <Route path="/condiciones" element={<Condiciones />} />

        {/* Rol Administrativo: /panel no choca con la administración interna de Django (/admin/) al recargar */}
        <Route path="/panel" element={<AdminDashboard />} />
        <Route path="/admin" element={<Navigate to="/panel" replace />} />

        {/* Fallback a Home */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}