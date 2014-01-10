

(function(/*! Stitch !*/) {
  if (!this.specs) {
    var modules = {}, cache = {}, require = function(name, root) {
      var path = expand(root, name), indexPath = expand(path, './index'), module, fn;
      module   = cache[path] || cache[indexPath]
      if (module) {
        return module.exports;
      } else if (fn = modules[path] || modules[path = indexPath]) {
        module = {id: path, exports: {}};
        try {
          cache[path] = module;
          fn(module.exports, function(name) {
            return require(name, dirname(path));
          }, module);
          return module.exports;
        } catch (err) {
          delete cache[path];
          throw err;
        }
      } else {
        throw 'module \'' + name + '\' not found';
      }
    }, expand = function(root, name) {
      var results = [], parts, part;
      if (/^\.\.?(\/|$)/.test(name)) {
        parts = [root, name].join('/').split('/');
      } else {
        parts = name.split('/');
      }
      for (var i = 0, length = parts.length; i < length; i++) {
        part = parts[i];
        if (part == '..') {
          results.pop();
        } else if (part != '.' && part != '') {
          results.push(part);
        }
      }
      return results.join('/');
    }, dirname = function(path) {
      return path.split('/').slice(0, -1).join('/');
    };
    this.specs = function(name) {
      return require(name, '');
    }
    this.specs.define = function(bundle) {
      for (var key in bundle)
        modules[key] = bundle[key];
    };
    this.specs.modules = modules;
    this.specs.cache   = cache;
  }
  return this.specs.define;
}).call(this)({
  "controllers/Users": function(exports, require, module) {(function() {
  var require;

  require = window.require;

  describe('The Users Controller', function() {
    var Users;
    Users = require('controllers/users');
    return it('can noop', function() {});
  });

}).call(this);
}, "models/Permission": function(exports, require, module) {(function() {
  var require;

  require = window.require;

  describe('The Permission Model', function() {
    var Permission;
    Permission = require('models/permission');
    return it('can noop', function() {});
  });

}).call(this);
}, "models/Role": function(exports, require, module) {(function() {
  var require;

  require = window.require;

  describe('The Role Model', function() {
    var Role;
    Role = require('models/role');
    return it('can noop', function() {});
  });

}).call(this);
}, "models/RolePermission": function(exports, require, module) {(function() {
  var require;

  require = window.require;

  describe('The RolePermission Model', function() {
    var RolePermission;
    RolePermission = require('models/rolepermission');
    return it('can noop', function() {});
  });

}).call(this);
}, "models/Token": function(exports, require, module) {(function() {
  var require;

  require = window.require;

  describe('The Token Model', function() {
    var Token;
    Token = require('models/token');
    return it('can noop', function() {});
  });

}).call(this);
}, "models/User": function(exports, require, module) {(function() {
  var require;

  require = window.require;

  describe('The User Model', function() {
    var User;
    User = require('models/user');
    return it('can noop', function() {});
  });

}).call(this);
}, "models/UserRole": function(exports, require, module) {(function() {
  var require;

  require = window.require;

  describe('The UserRole Model', function() {
    var UserRole;
    UserRole = require('models/userrole');
    return it('can noop', function() {});
  });

}).call(this);
}
});

require('lib/setup'); for (var key in specs.modules) specs(key);