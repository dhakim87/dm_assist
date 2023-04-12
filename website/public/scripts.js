$(document).ready(function(){
  $('.carousel').slick({
    dots: true,
    infinite: true,
    slidesToShow: 1,
    slidesToScroll: 1,
    autoplay: true,
    autoplaySpeed: 5000,
    arrows: false,
    responsive: [
      {
        breakpoint: 768,
        settings: {
          slidesToShow: 1,
          slidesToScroll: 1
        }
      },
      {
        breakpoint: 480,
        settings: {
          slidesToShow: 1,
          slidesToScroll: 1
        }
      }
    ]
  });
  
  var scroll = new SmoothScroll('a[href*="#"]', {
      speed: 800,
      speedAsDuration: true
    });

  // Apply AOS attributes to App Features section elements
  function applyAOS(selector, animation) {
    var elements = document.querySelectorAll(selector);
    elements.forEach(function (element) {
      element.setAttribute("data-aos", animation);
    });
  }

  // Call applyAOS function with desired selector and animation
  applyAOS(".app-features .feature-item", "fade-up");

  // Initialize AOS
  AOS.init({
    duration: 1200,
    once: true
  });
  
    
});
