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
        'main.js': /^js/,
        'javascripts/vendor.js': /^(bower_components|vendor)/
  
    stylesheets:
      joinTo:
        'main.css': /^js/,
        'css/vendor.css': /^(bower_components|vendor)/,
        'css/bootstrap.css': /^bower_components\/bootstrap\/dist\/css\/bootstrap\.css$/,
        'css/kendo.common.css': /^bower_components\/kendo-ui-core\/styles\/web\/kendo\.common\.core\.min\.css$/,
        'css/kendo.metro.css': /^bower_components\/kendo-ui-core\/styles\/web\/kendo\.metro\.min\.css$/
  
    templates:
      joinTo:
        'javascripts/tmpl.js': /^js/ # dirty hack for Jade compiling.  
  
  minify: yes      
