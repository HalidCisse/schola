exports.config =
  # See http://brunch.io/#documentation for docs.

  paths:
    public: 'public',
    watched: ['js']
  
  plugins:
    jade:
      pretty: yes # Adds pretty-indentation whitespaces to output (false by default)
  
  files:
    javascripts:
      joinTo:
        'school.js': /^(bower_components|vendor|js)/
      order:
        after: /^js/        
  
    stylesheets:
      joinTo:
        'school.css': /^(bower_components|vendor|js)/
      order:
        after: /^js/        
  
    templates:
      joinTo:
        'javascripts/school/tmpl.js': /^js/ # dirty hack for Jade compiling.
  
  minify: yes