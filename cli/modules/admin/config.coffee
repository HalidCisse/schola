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
        'admin.js': /^(bower_components|vendor|js)/
      order:
        after: /^js/
  
    stylesheets:
      joinTo:
        'admin.css': /^(bower_components|vendor|js)/
      order:
        after: /^js/        
  
    templates:
      joinTo:
        'javascripts/admin/tmpl.js': /^js/ # dirty hack for Jade compiling.  
  
  minify: yes