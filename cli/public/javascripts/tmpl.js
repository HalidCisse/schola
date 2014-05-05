(function(/*! Brunch !*/) {
  'use strict';

  var globals = typeof window !== 'undefined' ? window : global;
  if (typeof globals.require === 'function') return;

  var modules = {};
  var cache = {};

  var has = function(object, name) {
    return ({}).hasOwnProperty.call(object, name);
  };

  var expand = function(root, name) {
    var results = [], parts, part;
    if (/^\.\.?(\/|$)/.test(name)) {
      parts = [root, name].join('/').split('/');
    } else {
      parts = name.split('/');
    }
    for (var i = 0, length = parts.length; i < length; i++) {
      part = parts[i];
      if (part === '..') {
        results.pop();
      } else if (part !== '.' && part !== '') {
        results.push(part);
      }
    }
    return results.join('/');
  };

  var dirname = function(path) {
    return path.split('/').slice(0, -1).join('/');
  };

  var localRequire = function(path) {
    return function(name) {
      var dir = dirname(path);
      var absolute = expand(dir, name);
      return globals.require(absolute, path);
    };
  };

  var initModule = function(name, definition) {
    var module = {id: name, exports: {}};
    cache[name] = module;
    definition(module.exports, localRequire(name), module);
    return module.exports;
  };

  var require = function(name, loaderPath) {
    var path = expand(name, '.');
    if (loaderPath == null) loaderPath = '/';

    if (has(cache, path)) return cache[path].exports;
    if (has(modules, path)) return initModule(path, modules[path]);

    var dirIndex = expand(path, './index');
    if (has(cache, dirIndex)) return cache[dirIndex].exports;
    if (has(modules, dirIndex)) return initModule(dirIndex, modules[dirIndex]);

    throw new Error('Cannot find module "' + name + '" from '+ '"' + loaderPath + '"');
  };

  var define = function(bundle, fn) {
    if (typeof bundle === 'object') {
      for (var key in bundle) {
        if (has(bundle, key)) {
          modules[key] = bundle[key];
        }
      }
    } else {
      modules[bundle] = fn;
    }
  };

  var list = function() {
    var result = [];
    for (var item in modules) {
      if (has(modules, item)) {
        result.push(item);
      }
    }
    return result;
  };

  globals.require = require;
  globals.require.define = define;
  globals.require.register = define;
  globals.require.list = list;
  globals.require.brunch = true;
})();
require.register("js/views/label/form", function(exports, require, module) {
var __templateData = function template(locals) {
var buf = [];
var jade_mixins = {};
var jade_interp;
var locals_ = (locals || {}),form = locals_.form;
var jade_indent = [];
buf.push("\n<div class=\"modal\">\n  <div class=\"modal-dialog\">\n    <form id=\"label-form\"" + (jade.attr("role", form, true, false)) + " class=\"modal-content\">\n      <div class=\"modal-header\">\n        <p>\n          <button type=\"button\" data-dismiss=\"modal\" aria-hidden=\"true\" class=\"close pull-right\">×</button>\n        </p>\n        <div class=\"modal-title\"></div>\n      </div>\n      <div class=\"modal-body\">\n        <fieleset>\n          <div class=\"form-group\">\n            <input type=\"text\" name=\"name\" placeholder=\"New label name\" autofocus=\"autofocus\" required=\"required\" class=\"form-control\"/>\n            <input type=\"hidden\" name=\"color\" class=\"form-control\"/>\n            <ul class=\"color-chooser js-color-chooser\">\n              <li><span style=\"background-color: #e11d21 !important;\" data-hex-color=\"e11d21\" class=\"color-cooser-color js-color-cooser-color labelstyle-e11d21\"></span></li>\n              <li><span style=\"background-color: #eb6420 !important;\" data-hex-color=\"eb6420\" class=\"color-cooser-color js-color-cooser-color labelstyle-eb6420\"></span></li>\n              <li><span style=\"background-color: #fbca04 !important;\" data-hex-color=\"fbca04\" class=\"color-cooser-color js-color-cooser-color labelstyle-fbca04\"></span></li>\n              <li><span style=\"background-color: #009800 !important;\" data-hex-color=\"009800\" class=\"color-cooser-color js-color-cooser-color labelstyle-009800\"></span></li>\n              <li><span style=\"background-color: #006b75 !important;\" data-hex-color=\"006b75\" class=\"color-cooser-color js-color-cooser-color labelstyle-006b75\"></span></li>\n              <li><span style=\"background-color: #207de5 !important;\" data-hex-color=\"207de5\" class=\"color-cooser-color js-color-cooser-color labelstyle-207de5\"></span></li>\n              <li><span style=\"background-color: #0052cc !important;\" data-hex-color=\"0052cc\" class=\"color-cooser-color js-color-cooser-color labelstyle-0052cc\"></span></li>\n              <li><span style=\"background-color: #5319e7 !important;\" data-hex-color=\"5319e7\" class=\"color-cooser-color js-color-cooser-color labelstyle-5319e7\"></span></li>\n            </ul>\n            <ul class=\"color-chooser js-color-chooser\">\n              <li><span style=\"background-color: #f7c6c7 !important;\" data-hex-color=\"f7c6c7\" class=\"color-cooser-color js-color-cooser-color labelstyle-f7c6c7\"></span></li>\n              <li><span style=\"background-color: #fad8c7 !important;\" data-hex-color=\"fad8c7\" class=\"color-cooser-color js-color-cooser-color labelstyle-fad8c7\"></span></li>\n              <li><span style=\"background-color: #fef2c0 !important;\" data-hex-color=\"fef2c0\" class=\"color-cooser-color js-color-cooser-color labelstyle-fef2c0\"></span></li>\n              <li><span style=\"background-color: #bfe5bf !important;\" data-hex-color=\"bfe5bf\" class=\"color-cooser-color js-color-cooser-color labelstyle-bfe5bf\"></span></li>\n              <li><span style=\"background-color: #bfdadc !important;\" data-hex-color=\"bfdadc\" class=\"color-cooser-color js-color-cooser-color labelstyle-bfdadc\"></span></li>\n              <li><span style=\"background-color: #c7def8 !important;\" data-hex-color=\"c7def8\" class=\"color-cooser-color js-color-cooser-color labelstyle-c7def8\"></span></li>\n              <li><span style=\"background-color: #bfd4f2 !important;\" data-hex-color=\"bfd4f2\" class=\"color-cooser-color js-color-cooser-color labelstyle-bfd4f2\"></span></li>\n              <li><span style=\"background-color: #d4c5f9 !important;\" data-hex-color=\"d4c5f9\" class=\"color-cooser-color js-color-cooser-color labelstyle-d4c5f9\"></span></li>\n            </ul>\n          </div>\n        </fieleset>\n      </div>\n      <div class=\"modal-footer\">\n        <div class=\"btns pull-left\">\n          <button type=\"submit\" class=\"ok btn btn-primary\">Create</button>\n          <button type=\"button\" data-dismiss=\"modal\" class=\"btn btn-default\">Cancel</button>\n        </div>\n      </div>\n    </form>\n  </div>\n</div>");;return buf.join("");
};
if (typeof define === 'function' && define.amd) {
  define([], function() {
    return __templateData;
  });
} else if (typeof module === 'object' && module && module.exports) {
  module.exports = __templateData;
} else {
  __templateData;
}
});

;require.register("js/views/label/single", function(exports, require, module) {
var __templateData = function template(locals) {
var buf = [];
var jade_mixins = {};
var jade_interp;

var jade_indent = [];
buf.push("\n<div class=\"tag-item\">\n  <div class=\"color-wrapper\">\n    <div class=\"color\"></div>&nbsp;\n  </div>\n  <div class=\"name\"></div>\n  <div class=\"btn-group actions\">\n    <button type=\"button\" class=\"del btn btn-link btn-sm\">remove</button>\n    <button dtype=\"button\" class=\"edit btn btn-link btn-sm\">edit</button>\n  </div>\n</div>");;return buf.join("");
};
if (typeof define === 'function' && define.amd) {
  define([], function() {
    return __templateData;
  });
} else if (typeof module === 'object' && module && module.exports) {
  module.exports = __templateData;
} else {
  __templateData;
}
});

;require.register("js/views/label/tags", function(exports, require, module) {
var __templateData = function template(locals) {
var buf = [];
var jade_mixins = {};
var jade_interp;

var jade_indent = [];
buf.push("\n<div class=\"tags-wrapper\">\n  <div class=\"tags-header\">\n    <h3>Manage Labels</h3>\n  </div>\n  <div class=\"tags-toolbar\">\n    <div class=\"btn-group\">\n      <button data-toggle=\"tooltip\" data-placement=\"bottom\" title=\"Create new label\" type=\"button\" class=\"new btn btn-md btn-primary\"><span class=\"glyphicon glyphicon-plus\"></span></button>\n    </div>\n    <div class=\"btn-group filter\">\n      <input type=\"text\" placeholder=\"Filter\" autofocus=\"autofocus\" class=\"form-control squared input-md\"/><span class=\"glyphicon glyphicon-search\"></span>\n    </div>\n  </div>\n  <div class=\"shadow\"></div>\n  <div class=\"scrollable\">     \n    <div class=\"tags-list\"></div>\n  </div>\n</div>");;return buf.join("");
};
if (typeof define === 'function' && define.amd) {
  define([], function() {
    return __templateData;
  });
} else if (typeof module === 'object' && module && module.exports) {
  module.exports = __templateData;
} else {
  __templateData;
}
});

;require.register("js/views/menu/item", function(exports, require, module) {
var __templateData = function template(locals) {
var buf = [];
var jade_mixins = {};
var jade_interp;

var jade_indent = [];
buf.push("\n<li class=\"menuItem\">\n  <div class=\"item\"></div>\n  <div class=\"shadow up\"></div>\n  <ul class=\"children\"></ul>\n  <div class=\"shadow down\"></div>\n</li>");;return buf.join("");
};
if (typeof define === 'function' && define.amd) {
  define([], function() {
    return __templateData;
  });
} else if (typeof module === 'object' && module && module.exports) {
  module.exports = __templateData;
} else {
  __templateData;
}
});

;require.register("js/views/menu/link", function(exports, require, module) {
var __templateData = function template(locals) {
var buf = [];
var jade_mixins = {};
var jade_interp;
var locals_ = (locals || {}),href = locals_.href,countClass = locals_.countClass,count = locals_.count,icon = locals_.icon,title = locals_.title;
var jade_indent = [];
buf.push("<a" + (jade.attr("href", href, true, false)) + "><span" + (jade.cls(['badge','pull-right',countClass||''], [null,null,true])) + ">" + (jade.escape(null == (jade_interp = count||'') ? "" : jade_interp)) + "</span><span" + (jade.cls(['glyphicon',icon], [null,true])) + "></span><span class=\"title\">&nbsp;" + (jade.escape((jade_interp = title) == null ? '' : jade_interp)) + "</span></a>");;return buf.join("");
};
if (typeof define === 'function' && define.amd) {
  define([], function() {
    return __templateData;
  });
} else if (typeof module === 'object' && module && module.exports) {
  module.exports = __templateData;
} else {
  __templateData;
}
});

;require.register("js/views/menu/single", function(exports, require, module) {
var __templateData = function template(locals) {
var buf = [];
var jade_mixins = {};
var jade_interp;

var jade_indent = [];
buf.push("\n<h4 class=\"menuHeader\"></h4>\n<div>\n  <ul class=\"menuItems\"></ul>\n</div>");;return buf.join("");
};
if (typeof define === 'function' && define.amd) {
  define([], function() {
    return __templateData;
  });
} else if (typeof module === 'object' && module && module.exports) {
  module.exports = __templateData;
} else {
  __templateData;
}
});

;require.register("js/views/util/top-menu/list", function(exports, require, module) {
var __templateData = function template(locals) {
var buf = [];
var jade_mixins = {};
var jade_interp;

var jade_indent = [];
// iterate locals
;(function(){
  var $$obj = locals;
  if ('number' == typeof $$obj.length) {

    for (var $index = 0, $$l = $$obj.length; $index < $$l; $index++) {
      var val = $$obj[$index];

buf.push("\n<li class=\"item\"><a" + (jade.attr("href", val.defaultUrl, true, false)) + ">" + (jade.escape((jade_interp = this.S(val.name).capitalize().s) == null ? '' : jade_interp)) + "</a></li>");
    }

  } else {
    var $$l = 0;
    for (var $index in $$obj) {
      $$l++;      var val = $$obj[$index];

buf.push("\n<li class=\"item\"><a" + (jade.attr("href", val.defaultUrl, true, false)) + ">" + (jade.escape((jade_interp = this.S(val.name).capitalize().s) == null ? '' : jade_interp)) + "</a></li>");
    }

  }
}).call(this);
;return buf.join("");
};
if (typeof define === 'function' && define.amd) {
  define([], function() {
    return __templateData;
  });
} else if (typeof module === 'object' && module && module.exports) {
  module.exports = __templateData;
} else {
  __templateData;
}
});

;
//# sourceMappingURL=tmpl.js.map