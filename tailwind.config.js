/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        canvas: '#FAF7F2',
        surface: {
          DEFAULT: '#FAF7F2',
          raised: '#FFFFFF',
          border: '#EBE5DF',
          dim: '#DCDAD5',
        },
        ink: {
          DEFAULT: '#14213D',
          subdued: '#7A7571',
          light: '#4A4541',
        },
        amber: {
          bookplate: '#E09F3E',
          soft: '#FEB956',
          container: '#FFF4E5',
        },
        pine: {
          DEFAULT: '#2D6A4F',
          soft: '#40916C',
          container: '#E8F5EE',
        },
        terracotta: {
          DEFAULT: '#B83A24',
          soft: '#FFDAD6',
        }
      },
      fontFamily: {
        serif: ['"Source Serif 4"', 'serif'],
        sans: ['Inter', 'sans-serif'],
      },
      borderRadius: {
        'card': '16px',
        'control': '12px',
        'pill': '9999px',
      },
      boxShadow: {
        'card': '0px 2px 6px rgba(20, 33, 61, 0.04)',
        'modal': '0px 8px 24px rgba(20, 33, 61, 0.08)',
        'floating': '0px 4px 16px rgba(20, 33, 61, 0.12)',
      }
    },
  },
  plugins: [],
};
