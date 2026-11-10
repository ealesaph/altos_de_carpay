/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        forest: {
          950: '#0f1d15',
          900: '#172e21', // Verde muy oscuro del footer y botones
          850: '#1e3a2b',
          800: '#234433', // Verde institucional de botones principales
          700: '#2d5741',
          100: '#eaf1ec', // Fondo verde claro suave
          50: '#f4f7f5',
        },
        sand: {
          50: '#faf8f5',  // Fondo base crema claro del diseño
          100: '#f2ede4',
          200: '#e5ded1',
        }
      },
      fontFamily: {
        serif: ['Georgia', 'Cambria', 'Times New Roman', 'serif'],
        sans: ['system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
      }
    },
  },
  plugins: [],
}