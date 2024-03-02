/** @type {import('tailwindcss').Config} */

const colors = require('tailwindcss/colors')

module.exports = {
  content: ["./src/**/*.{html,js,clj}"],
  theme: {
      extend: {
          keyframes: {
              'pulse-amber': {
                  '0%, 100%': {'opacity': '1',},
                  // color is amber-400
                  '50%': {'opacity': '.5', 'background-color': '#fbbf24'}
              }
          },
          animation: {
              'pulse-amber': 'pulse-amber 2s cubic-bezier(0.4, 0, 0.6, 1) infinite',
              'ping-2s': 'ping 2s cubic-bezier(0, 0, 0.2, 1) infinite'
          }
      },
  },
  plugins: [
    require('@tailwindcss/typography'),
    require('@tailwindcss/forms'),
    require('@tailwindcss/aspect-ratio'),
  ],
}
