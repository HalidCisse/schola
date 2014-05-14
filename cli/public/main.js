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
require.register("js/controllers/LabelForm", function(exports, require, module) {
var LabelForm,
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

LabelForm = (function(_super) {
  __extends(LabelForm, _super);

  LabelForm.elements = {
    '[name=name]': 'name',
    '[name=color]': 'color'
  };

  LabelForm.events = {
    'submit form': 'save'
  };

  function LabelForm() {
    this.el = require('js/views/label/form')();
    LabelForm.__super__.constructor.apply(this, arguments);
    this.$('.color-cooser-color').click((function(_this) {
      return function(evt) {
        evt.preventDefault();
        evt.stopPropagation();
        return _this.color.val(_this.$(evt.target).data('hex-color'));
      };
    })(this));
  }

  LabelForm.prototype.New = function() {
    this.label = void 0;
    this.$('.modal-title').html('New Label');
    this.name.val('');
    this.color.val('#fff');
    this.el.modal();
    return this.delay(function() {
      return this.name.focus();
    });
  };

  LabelForm.prototype.Edit = function(label) {
    this.label = label;
    this.$('.modal-title').html('Edit Label');
    this.name.val(this.label.name);
    this.color.val(this.label.color);
    this.el.modal();
    return this.delay(function() {
      return this.name.focus();
    });
  };

  LabelForm.prototype.save = function(e) {
    e.preventDefault();
    this.el.modal('hide');
    if (this.label) {
      return this.trigger('update', this.label.name, this.getInfo());
    } else {
      return this.trigger('save', this.getInfo());
    }
  };

  LabelForm.prototype.getInfo = function() {
    return {
      name: app.cleaned(this.name),
      color: app.cleaned(this.color)
    };
  };

  return LabelForm;

})(Spine.Controller);

module.exports = LabelForm;
});

;require.register("js/controllers/Menu", function(exports, require, module) {
var Menu,
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; },
  __slice = [].slice;

Menu = (function(_super) {
  __extends(Menu, _super);

  Menu.prototype.className = 'menu';

  Menu.elements = {
    '.menuHeader': 'menuHeader',
    '.menuItems': 'menuItems'
  };

  function Menu() {
    Menu.__super__.constructor.apply(this, arguments);
    this.manager = new Menu.Manager;
    this.render();
  }

  Menu.tmpl = require('js/views/menu/single')();

  Menu.itemTmpl = require('js/views/menu/item')();

  Menu.prototype.render = function() {
    var item, _i, _len, _ref;
    this.html(Menu.tmpl);
    this.el.attr('id', "menu-" + this.id);
    this.el.addClass("menu-" + this.id);
    this.menuHeader.html(this.header);
    _ref = this.items;
    for (_i = 0, _len = _ref.length; _i < _len; _i++) {
      item = _ref[_i];
      this.menuItems.append(this.add(new Menu.Item({
        item: item,
        manager: this.manager,
        menu: this,
        el: Menu.itemTmpl
      })).render());
    }
    return this.el;
  };

  Menu.prototype.getItem = function(id, clbk) {
    var _, _i, _len, _ref;
    _ref = this.manager.items;
    for (_i = 0, _len = _ref.length; _i < _len; _i++) {
      _ = _ref[_i];
      if (_.item.id === id) {
        clbk(_);
        return;
      } else if (_.item.getChildren) {
        _.item.getChildren().done((function(_this) {
          return function(children) {
            var ch, _j, _len1, _results;
            _results = [];
            for (_j = 0, _len1 = children.length; _j < _len1; _j++) {
              ch = children[_j];
              if (ch.id === id) {
                _results.push(clbk(ch));
              }
            }
            return _results;
          };
        })(this));
      }
    }
  };

  Menu.prototype.add = function(item) {
    this.manager.add(item);
    return item;
  };

  Menu.prototype.activateItem = function(item) {
    return this.manager.activate(item);
  };

  Menu.Manager = (function(_super1) {
    __extends(Manager, _super1);

    Manager.include(Spine.Events);

    function Manager() {
      this.items = [];
      this.bind('change', this.change);
      this.add.apply(this, arguments);
    }

    Manager.prototype.add = function() {
      var item, items, _i, _len, _results;
      items = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
      _results = [];
      for (_i = 0, _len = items.length; _i < _len; _i++) {
        item = items[_i];
        _results.push(this.addOne(item));
      }
      return _results;
    };

    Manager.prototype.addOne = function(item) {
      item.bind('active', (function(_this) {
        return function() {
          var args;
          args = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
          return _this.trigger.apply(_this, ['change', item].concat(__slice.call(args)));
        };
      })(this));
      item.bind('release', (function(_this) {
        return function() {
          return _this.items.splice(_this.items.indexOf(item), 1);
        };
      })(this));
      return this.items.push(item);
    };

    Manager.prototype.activate = function(item) {
      this.trigger.apply(this, ['change', item].concat(__slice.call(arguments)));
      return item;
    };

    Manager.prototype.change = function() {
      var args, current, item, _i, _len, _ref;
      current = arguments[0], args = 2 <= arguments.length ? __slice.call(arguments, 1) : [];
      _ref = this.items;
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        item = _ref[_i];
        if (item !== current) {
          item.deactivate.apply(item, args);
        }
      }
      if (current) {
        return current.activate.apply(current, args);
      }
    };

    return Manager;

  })(Spine.Module);

  Menu.Item = (function(_super1) {
    __extends(Item, _super1);

    Item.prototype.elements = {
      '.item': 'itemElement',
      '.children': 'childElements'
    };

    Item.events = {
      'click': 'clicked'
    };

    function Item() {
      Item.__super__.constructor.apply(this, arguments);
      this.el.addClass("menu-item-" + (this.item.id.replace('.', '_')));
    }

    Item.prototype.loading = function() {
      NProgress.start();
      this.itemElement.addClass('loading');
      return this;
    };

    Item.prototype.doneLoading = function() {
      NProgress.done();
      this.itemElement.removeClass('loading');
      return this;
    };

    Item.prototype.activate = function() {
      this.itemElement.addClass('active');
      if (this.item.state) {
        this.menu.el.addClass(this.item.state);
      }
      return this;
    };

    Item.prototype.deactivate = function() {
      this.itemElement.removeClass('active');
      if (this.item.state) {
        this.menu.el.removeClass(this.item.state);
      }
      return this;
    };

    Item.prototype.clicked = function(e) {
      this.delay(function() {
        return this.trigger('active');
      });
      e.stopPropagation();
      e.preventDefault();
      return this.navigate(this.item.href);
    };

    Item.linkTmpl = require('js/views/menu/link');

    Item.prototype.add = function(item) {
      this.manager.add(item);
      return item;
    };

    Item.prototype.render = function() {
      this.itemElement.html((this.item.render || Menu.Item.linkTmpl)(this.item));
      if (this.item.getChildren) {
        this.item.getChildren().done((function(_this) {
          return function(children) {
            var child, _i, _len, _results;
            _results = [];
            for (_i = 0, _len = children.length; _i < _len; _i++) {
              child = children[_i];
              _results.push(_this.childElements.append(_this.add(new Menu.Item({
                item: child,
                manager: _this.manager,
                menu: _this,
                el: Menu.itemTmpl
              })).render()));
            }
            return _results;
          };
        })(this));
      }
      return this.el;
    };

    return Item;

  })(Spine.Controller);

  Menu.Mgr = (function() {
    function Mgr() {}

    Mgr.prototype.menus = function() {
      return this._menus;
    };

    Mgr.prototype.routes = function() {
      return {};
    };

    Mgr.prototype._menus = {};

    Mgr.prototype.add = function(opts) {
      this.rm(opts.id);
      return this._menus[opts.id] = new Menu(opts);
    };

    Mgr.prototype.rm = function(id) {
      if (this._menus[id]) {
        this._menus[id].release();
        return delete this._menus[id];
      }
    };

    Mgr.prototype.activate = function(id, clbk) {
      return this._getMenu(id, function(arg) {
        var item, menu, _;
        menu = arg.menu, item = arg.item;
        menu.activateItem(item);
        try {
          return clbk(item);
        } catch (_error) {
          _ = _error;
        }
      });
    };

    Mgr.prototype._getMenu = function(id, clbk) {
      var menu, _, _ref, _results;
      _ref = this._menus;
      _results = [];
      for (_ in _ref) {
        menu = _ref[_];
        _results.push(menu.getItem(id, (function(_this) {
          return function(item) {
            if (item) {
              return clbk({
                menu: menu,
                item: item
              });
            }
          };
        })(this)));
      }
      return _results;
    };

    return Mgr;

  })();

  return Menu;

})(Spine.Controller);

module.exports = Menu;
});

;require.register("js/controllers/Tag", function(exports, require, module) {
var LabelForm, Tag, TagM,
  __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

TagM = require('js/models/Tag');

LabelForm = require('js/controllers/LabelForm');

Tag = (function() {
  function Tag() {}

  return Tag;

})();

Tag.Mgr = (function(_super) {
  __extends(Mgr, _super);

  Mgr.prototype.logPrefix = '(Tag.Mgr)';

  Mgr.prototype.className = 'labels';

  Mgr.elements = {
    '.tags-list': 'tagList',
    '.filter input': 'filterInput'
  };

  Mgr.events = {
    'click .new': 'New',
    'input .filter': 'Filter'
  };

  function Mgr() {
    this.Refresh = __bind(this.Refresh, this);
    Mgr.__super__.constructor.apply(this, arguments);
    this.render();
    this.active(function() {
      this.log("active");
      return this.delay((function(_this) {
        return function() {
          return TagM.Reload();
        };
      })(this));
    });
    TagM.on('refresh', this.Refresh);
    this.on('edit', this.Edit);
    this.on('del', this.Del);
    Mgr.form.on('save', this.saveTag);
    Mgr.form.on('update', this.updateTag);
  }

  Mgr.prototype.tags = [];

  Mgr.prototype.Refresh = function() {
    var item, lastIndex, _i, _len, _ref, _results;
    while (lastIndex = this.tags.length) {
      this.tags[lastIndex - 1].release();
    }
    _ref = TagM.all();
    _results = [];
    for (_i = 0, _len = _ref.length; _i < _len; _i++) {
      item = _ref[_i];
      _results.push(this.tagList.append(this.add(new Mgr.Row({
        item: item,
        el: Mgr.rowTmpl,
        q: this.filterInput.val(),
        mgr: this
      })).el));
    }
    return _results;
  };

  Mgr.prototype.add = function(tag) {
    var index;
    index = this.tags.push(tag);
    tag.on('release', (function(_this) {
      return function() {
        return _this.tags.splice(index - 1, 1);
      };
    })(this));
    return tag;
  };

  Mgr.prototype.Edit = function(item) {
    return Mgr.form.Edit(item);
  };

  Mgr.prototype.Del = function(item) {
    return this.log("Del " + item);
  };

  Mgr.prototype.saveTag = function(info) {
    return this.log("saveTag " + (JSON.stringify(info)));
  };

  Mgr.prototype.updateTag = function(name, info) {
    return this.log("updateTag " + name + " : " + (JSON.stringify(info)));
  };

  Mgr.prototype.render = function() {
    return this.html(Mgr.tmpl);
  };

  Mgr.tmpl = require('js/views/label/tags')();

  Mgr.rowTmpl = require('js/views/label/single')();

  Mgr.prototype.New = function(e) {
    e.preventDefault();
    e.stopPropagation();
    return Mgr.form.New();
  };

  Mgr.prototype.Filter = function() {
    var q, tag, _i, _len, _ref, _results;
    q = this.filterInput.val();
    _ref = this.tags;
    _results = [];
    for (_i = 0, _len = _ref.length; _i < _len; _i++) {
      tag = _ref[_i];
      _results.push(tag.Filtered(q));
    }
    return _results;
  };

  Mgr.Row = (function(_super1) {
    __extends(Row, _super1);

    Row.events = {
      'click .edit': 'Edit',
      'click .del': 'Del'
    };

    function Row() {
      Row.__super__.constructor.apply(this, arguments);
      this.item.on('change', this.FillData);
      this.FillData();
    }

    Row.prototype.Filtered = function(q) {
      var found;
      this.q = q;
      found = false;
      this.$('.color').css('background-color', this.item.color);
      this.$('.name').html(this.item.name.replace(new RegExp("^(.*)(" + this.q + ")(.*)$"), function(name, $1, $2, $3) {
        if (!!$2) {
          found = true;
          return "" + $1 + "<cite class='q'>" + $2 + "</cite>" + $3;
        } else {
          return name;
        }
      }));
      return found;
    };

    Row.prototype.FillData = function() {
      if (this.q !== void 0) {
        this.Filtered(this.q);
      } else {
        this.$('.color').css('background-color', this.item.color);
        this.$('.name').html(this.item.name);
      }
      return this.el;
    };

    Row.prototype.Edit = function(e) {
      e.preventDefault();
      e.stopPropagation();
      return this.mgr.trigger('edit', this.item);
    };

    Row.prototype.Del = function(e) {
      e.preventDefault();
      e.stopPropagation();
      return this.mgr.trigger('del', this.item);
    };

    return Row;

  })(Spine.Controller);

  Mgr.form = new LabelForm;

  return Mgr;

})(Spine.Controller);

Tag.Stack = (function(_super) {
  __extends(Stack, _super);

  Stack.prototype.logPrefix = '(Tag.Stack)';

  Stack.prototype.className = 'spine stack labels';

  Stack.prototype.controllers = {
    mgr: Tag.Mgr
  };

  function Stack() {
    this.routes = {
      '/labels': 'mgr'
    };
    Stack.__super__.constructor.apply(this, arguments);
  }

  return Stack;

})(Spine.Stack);

module.exports = Tag;
});

;require.register("js/index", function(exports, require, module) {
var App, Session, Tag,
  __slice = [].slice,
  __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

Session = require('js/lib/session');

Tag = require('js/controllers/Tag');


/* 

 * Hack to support hierarchy of stacks

 * Activating a child will activate all its direct parent stacks
 * and deactivate all siblings (controllers or stacks), their parents and children recursively
 */

Spine.Manager.prototype.change = function() {
  var args, cont, current, deactivate, parent, _i, _len, _ref, _ref1;
  current = arguments[0], args = 2 <= arguments.length ? __slice.call(arguments, 1) : [];
  deactivate = function(cont) {
    var child, _i, _len, _ref, _results;
    cont.deactivate.apply(cont, args);
    if (cont.controllers) {
      _ref = cont.controllers;
      _results = [];
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        child = _ref[_i];
        _results.push(deactivate(child));
      }
      return _results;
    }
  };
  _ref = this.controllers;
  for (_i = 0, _len = _ref.length; _i < _len; _i++) {
    cont = _ref[_i];
    if (cont !== current) {
      deactivate(cont);
    }
  }
  if (current) {
    if (current.stack && (parent = current.stack.stack)) {
      (_ref1 = parent.manager).trigger.apply(_ref1, ['change', current.stack].concat(__slice.call(args)));
    }
    return current.activate.apply(current, args);
  }
};

App = (function(_super) {
  __extends(App, _super);

  App.include(Spine.Log);

  App.include(Spine.Events);

  App.prototype.isLoggedIn = function() {
    return !!this.sesssion.key;
  };

  App.prototype.hostname = 'localhost';

  App.prototype.port = 80;

  App.prototype.server = "http://" + App.prototype.hostname + (App.prototype.port ? ':' + App.prototype.port : '');

  App.prototype.session = {};

  App.prototype.labels = require('js/lib/labels');

  App.prototype.redirect = function(path) {
    return Spine.Route.redirect(path);
  };

  App.prototype.cleaned = function(k) {
    k = k.val();
    return (k && k.trim()) || '';
  };

  App.prototype.modules = {};

  App.prototype.getApps = function() {
    return $.getJSON(jsRoutes.controllers.Utils.getApps().url);
  };

  App.prototype.registerApp = function(app) {
    var key, route, val, _i, _len, _ref, _ref1, _ref2, _results;
    this.topMenu.addApp(app.name, app.defaultUrl);
    this.menu.addMenu(app.name, app.menu.controller);
    _ref = app.menu.routes;
    for (_i = 0, _len = _ref.length; _i < _len; _i++) {
      route = _ref[_i];
      this.menu.addRoute(route.path, route.callback.bind(this.menu));
    }
    _ref1 = app.controllers;
    for (key in _ref1) {
      val = _ref1[key];
      this.view.addView(key, val);
    }
    _ref2 = app.routes;
    _results = [];
    for (key in _ref2) {
      val = _ref2[key];
      _results.push(this.view.addRoute(key, val));
    }
    return _results;
  };

  App.prototype.onLoad = function() {
    var _, _app, _ref, _ref1;
    this.topMenu.addApp('archives', '#/archives');
    this.topMenu.addApp('schools', '#/schools');
    this.topMenu.render();
    this.on('session.loggedin', (function(_this) {
      return function(s) {
        if (s.suspended || s.changePasswordAtNextLogin) {
          return setTimeout(function() {
            return this.redirect('/');
          });
        }
        return _this.session = s;
      };
    })(this));
    this.on('session.error', (function(_this) {
      return function() {
        _this.session = {};
        return $.ajaxSetup({
          beforeSend: $.noop
        });
      };
    })(this));
    this.on('session.loggedout', (function(_this) {
      return function() {
        return _this.redirect('/');
      };
    })(this));
    Spine.Route.setup({
      redirect: (function(_this) {
        return function() {
          return _this.navigate('/');
        };
      })(this)
    });
    if ((_ref = Spine.Route.getPath()) === '/' || _ref === '') {
      _ref1 = this.modules;
      for (_ in _ref1) {
        _app = _ref1[_];
        this.menu.delay(function() {
          return this.navigate(_app.defaultUrl);
        });
        break;
      }
    }
    this.menu.delay(function() {
      return $('.scrollable').on('scroll', function() {
        return $(this).siblings('.shadow').css({
          top: $(this).siblings('.toolbar').outerHeight(),
          opacity: this.scrollTop === 0 ? 0 : 0.8
        });
      });
    });
    return $(document.body).addClass('loaded');
  };

  function App() {
    this.onLoad = __bind(this.onLoad, this);
    App.__super__.constructor.apply(this, arguments);
    this.on('loaded', this.onLoad);
    this.sessionMgr = new Session;
    this.view = new App.Stack;
    this.menu = new App.Menu;
    this.topMenu = new App.TopMenu;
  }

  App.TopMenu = (function(_super1) {
    __extends(TopMenu, _super1);

    TopMenu.prototype.apps = [];

    TopMenu.tmpl = require('js/views/util/top-menu/list').bind(window);

    function TopMenu(args) {
      if (args == null) {
        args = {
          el: '#top-menu'
        };
      }
      this.addApp = __bind(this.addApp, this);
      this.changeApp = __bind(this.changeApp, this);
      TopMenu.__super__.constructor.call(this, args);
      this.list = new Spine.List({
        template: TopMenu.tmpl,
        selectFirst: true,
        className: 'dropdown-menu squared',
        attributes: {
          role: 'menu'
        }
      });
      this.list.on('change', this.changeApp);
      this.append(this.list.el);
    }

    TopMenu.prototype.changeApp = function(app) {
      this.$('a.current-app').attr('href', app.defaultUrl);
      return this.$('a.current-app > .navbar-brand').html(S(app.name).capitalize().s);
    };

    TopMenu.prototype.render = function() {
      this.el.addClass("top-menu-count-" + this.apps.length);
      return this.list.render(this.apps);
    };

    TopMenu.prototype.addApp = function(name, defaultUrl) {
      return this.apps.push({
        name: name,
        defaultUrl: defaultUrl
      });
    };

    return TopMenu;

  })(Spine.Controller);

  App.Menu = (function(_super1) {
    __extends(Menu, _super1);

    Menu.prototype.className = 'menus spine stack';

    function Menu(args) {
      if (args == null) {
        args = {
          el: '#sidebar'
        };
      }
      this.addRoute = __bind(this.addRoute, this);
      this.addMenu = __bind(this.addMenu, this);
      Menu.__super__.constructor.call(this, args);
    }

    Menu.prototype.addMenu = function(key, val) {
      val.stack = this;
      this[key] = val;
      return this.add(this[key]);
    };

    Menu.prototype.addRoute = function(key, val) {
      var callback;
      if (typeof val === 'function') {
        callback = val;
      }
      callback || (callback = (function(_this) {
        return function() {
          var _ref;
          return (_ref = _this[val]).active.apply(_ref, arguments);
        };
      })(this));
      return this.route(key, callback);
    };

    return Menu;

  })(Spine.Stack);

  App.Stack = (function(_super1) {
    __extends(Stack, _super1);

    Stack.prototype.controllers = {
      labels: Tag.Stack
    };

    Stack.prototype.className = 'app spine stack';

    function Stack(args) {
      if (args == null) {
        args = {
          el: '#content'
        };
      }
      this.addRoute = __bind(this.addRoute, this);
      this.addView = __bind(this.addView, this);
      Stack.__super__.constructor.call(this, args);
    }

    Stack.prototype.addView = function(key, val) {
      this[key] = new val({
        stack: this
      });
      return this.add(this[key]);
    };

    Stack.prototype.addRoute = function(key, val) {
      var callback;
      if (typeof val === 'function') {
        callback = val;
      }
      callback || (callback = (function(_this) {
        return function() {
          var _ref;
          return (_ref = _this[val]).active.apply(_ref, arguments);
        };
      })(this));
      return this.route(key, callback);
    };

    return Stack;

  })(Spine.Stack);

  App.prototype.arraysEqual = function(a, b) {
    var i, _i, _ref;
    if (a === b) {
      return true;
    }
    if (a === null || b === null) {
      return false;
    }
    if (a.length !== b.length) {
      return false;
    }
    a.sort();
    b.sort();
    for (i = _i = 0, _ref = a.length; 0 <= _ref ? _i <= _ref : _i >= _ref; i = 0 <= _ref ? ++_i : --_i) {
      if (a[i] !== b[i]) {
        return false;
      }
    }
    return true;
  };

  return App;

})(Spine.Module);

module.exports = App;
});

;require.register("js/lib/labels", function(exports, require, module) {
var R, labels;

R = jsRoutes.controllers.Tags;

labels = (function() {
  function labels() {}

  labels.getLabels = function() {
    return $.getJSON(R.getTags().url);
  };

  labels.addLabel = function(label, color) {
    var route;
    route = R.addTag(label, color);
    return $.ajax({
      type: route.type,
      url: route.url,
      dataType: 'json'
    });
  };

  labels.updateLabelColor = function(name, color) {
    var route;
    route = R.updateTagColor(name, color);
    return $.ajax({
      type: route.type,
      url: route.url,
      dataType: 'json'
    });
  };

  labels.updateLabel = function(name, newName) {
    var route;
    route = R.updateTag(name, newName);
    return $.ajax({
      type: route.type,
      url: route.url,
      dataType: 'json'
    });
  };

  labels.purgeLabels = function(labels) {
    var route;
    route = R.purgeTags(labels);
    return $.ajax({
      type: route.type,
      url: route.url,
      dataType: 'json'
    });
  };

  return labels;

})();

module.exports = labels;
});

;require.register("js/lib/mac", function(exports, require, module) {
var Mac;

Mac = (function() {
  function Mac() {}

  Mac._genNonce = function(issuedAt) {
    return "" + (Date.now() - issuedAt) + ":" + (Crypto.util.bytesToHex(Crypto.util.randomBytes(128 / 32)));
  };

  Mac.sign = function(secret, rs) {
    return Crypto.util.bytesToBase64(Crypto.HMAC(Crypto.SHA1, rs, secret, {
      asBytes: true
    }));
  };

  Mac.toReqString = function(nonce, method, uri, hostname, port, bodyHash, ext) {
    return [nonce, method, uri, hostname, port, bodyHash || "", ext || ""].join("\n") + "\n";
  };

  Mac.createHeader = function(session, xhr, req) {
    var mac, nonce, rs;
    nonce = Mac._genNonce(parseInt(session.issuedTime));
    rs = Mac.toReqString(nonce, req.type, req.url, app.hostname || 'localhost', app.port || 80);
    mac = Mac.sign(session.secret, rs);
    return "MAC id=\"" + (session.access_token || session.key) + "\",nonce=\"" + nonce + "\",mac=\"" + mac + "\"";
  };

  return Mac;

})();

module.exports = Mac;
});

;require.register("js/lib/position", function(exports, require, module) {

// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview This file provides utility functions for position popups.
 */

/**
 * Type def for rects as returned by getBoundingClientRect.
 * @typedef { {left: number, top: number, width: number, height: number,
 *             right: number, bottom: number}}
 */
var Rect;

/**
 * Enum for defining how to anchor a popup to an anchor element.
 * @enum {number}
 */
var AnchorType = {
  /**
   * The popup's right edge is aligned with the left edge of the anchor.
   * The popup's top edge is aligned with the top edge of the anchor.
   */
  BEFORE: 1,  // p: right, a: left, p: top, a: top

  /**
   * The popop's left edge is aligned with the right edge of the anchor.
   * The popup's top edge is aligned with the top edge of the anchor.
   */
  AFTER: 2,  // p: left a: right, p: top, a: top

  /**
   * The popop's bottom edge is aligned with the top edge of the anchor.
   * The popup's left edge is aligned with the left edge of the anchor.
   */
  ABOVE: 3,  // p: bottom, a: top, p: left, a: left

  /**
   * The popop's top edge is aligned with the bottom edge of the anchor.
   * The popup's left edge is aligned with the left edge of the anchor.
   */
  BELOW: 4  // p: top, a: bottom, p: left, a: left
};

/**
 * Helper function for positionPopupAroundElement and positionPopupAroundRect.
 * @param {!Rect} anchorRect The rect for the anchor.
 * @param {!HTMLElement} popupElement The element used for the popup.
 * @param {AnchorType} type The type of anchoring to do.
 * @param {boolean} invertLeftRight Whether to invert the right/left
 *     alignment.
 */
function positionPopupAroundRect(anchorRect, popupElement, type,
                                 invertLeftRight) {
  var popupRect = popupElement.getBoundingClientRect();
  var availRect;
  var ownerDoc = popupElement.ownerDocument;
  var cs = ownerDoc.defaultView.getComputedStyle(popupElement);
  var docElement = ownerDoc.documentElement;

  if (cs.position == 'fixed') {
    // For 'fixed' positioned popups, the available rectangle should be based
    // on the viewport rather than the document.
    availRect = {
      height: docElement.clientHeight,
      width: docElement.clientWidth,
      top: 0,
      bottom: docElement.clientHeight,
      left: 0,
      right: docElement.clientWidth
    };
  } else {
    availRect = popupElement.offsetParent.getBoundingClientRect();
  }

  if (cs.direction == 'rtl')
    invertLeftRight = !invertLeftRight;

  // Flip BEFORE, AFTER based on alignment.
  if (invertLeftRight) {
    if (type == AnchorType.BEFORE)
      type = AnchorType.AFTER;
    else if (type == AnchorType.AFTER)
      type = AnchorType.BEFORE;
  }

  // Flip type based on available size
  switch (type) {
    case AnchorType.BELOW:
      if (anchorRect.bottom + popupRect.height > availRect.height &&
          popupRect.height <= anchorRect.top) {
        type = AnchorType.ABOVE;
      }
      break;
    case AnchorType.ABOVE:
      if (popupRect.height > anchorRect.top &&
          anchorRect.bottom + popupRect.height <= availRect.height) {
        type = AnchorType.BELOW;
      }
      break;
    case AnchorType.AFTER:
      if (anchorRect.right + popupRect.width > availRect.width &&
          popupRect.width <= anchorRect.left) {
        type = AnchorType.BEFORE;
      }
      break;
    case AnchorType.BEFORE:
      if (popupRect.width > anchorRect.left &&
          anchorRect.right + popupRect.width <= availRect.width) {
        type = AnchorType.AFTER;
      }
      break;
  }
  // flipping done

  var style = popupElement.style;
  // Reset all directions.
  style.left = style.right = style.top = style.bottom = 'auto';

  // Primary direction
  switch (type) {
    case AnchorType.BELOW:
      if (anchorRect.bottom + popupRect.height <= availRect.height)
        style.top = anchorRect.bottom + 'px';
      else
        style.bottom = '0';
      break;
    case AnchorType.ABOVE:
      if (availRect.height - anchorRect.top >= 0)
        style.bottom = availRect.height - anchorRect.top + 'px';
      else
        style.top = '0';
      break;
    case AnchorType.AFTER:
      if (anchorRect.right + popupRect.width <= availRect.width)
        style.left = anchorRect.right + 'px';
      else
        style.right = '0';
      break;
    case AnchorType.BEFORE:
      if (availRect.width - anchorRect.left >= 0)
        style.right = availRect.width - anchorRect.left + 'px';
      else
        style.left = '0';
      break;
  }

  // Secondary direction
  switch (type) {
    case AnchorType.BELOW:
    case AnchorType.ABOVE:
      if (invertLeftRight) {
        // align right edges
        if (anchorRect.right - popupRect.width >= 0) {
          style.right = availRect.width - anchorRect.right + 'px';

        // align left edges
        } else if (anchorRect.left + popupRect.width <= availRect.width) {
          style.left = anchorRect.left + 'px';

        // not enough room on either side
        } else {
          style.right = '0';
        }
      } else {
        // align left edges
        if (anchorRect.left + popupRect.width <= availRect.width) {
          style.left = anchorRect.left + 'px';

        // align right edges
        } else if (anchorRect.right - popupRect.width >= 0) {
          style.right = availRect.width - anchorRect.right + 'px';

        // not enough room on either side
        } else {
          style.left = '0';
        }
      }
      break;

    case AnchorType.AFTER:
    case AnchorType.BEFORE:
      // align top edges
      if (anchorRect.top + popupRect.height <= availRect.height) {
        style.top = anchorRect.top + 'px';

      // align bottom edges
      } else if (anchorRect.bottom - popupRect.height >= 0) {
        style.bottom = availRect.height - anchorRect.bottom + 'px';

        // not enough room on either side
      } else {
        style.top = '0';
      }
      break;
  }
}

/**
 * Positions a popup element relative to an anchor element. The popup element
 * should have position set to absolute and it should be a child of the body
 * element.
 * @param {!HTMLElement} anchorElement The element that the popup is anchored
 *     to.
 * @param {!HTMLElement} popupElement The popup element we are positioning.
 * @param {AnchorType} type The type of anchoring we want.
 * @param {boolean} invertLeftRight Whether to invert the right/left
 *     alignment.
 */
function positionPopupAroundElement(anchorElement, popupElement, type,
                                    invertLeftRight) {
  var anchorRect = anchorElement.getBoundingClientRect();
  positionPopupAroundRect(anchorRect, popupElement, type, invertLeftRight);
}

/**
 * Positions a popup around a point.
 * @param {number} x The client x position.
 * @param {number} y The client y position.
 * @param {!HTMLElement} popupElement The popup element we are positioning.
 */
function positionPopupAtPoint(x, y, popupElement) {
  var rect = {
    left: x,
    top: y,
    width: 0,
    height: 0,
    right: x,
    bottom: y
  };
  positionPopupAroundRect(rect, popupElement, AnchorType.BELOW);
}

module.exports = {
  AnchorType: AnchorType,
  positionPopupAroundElement: positionPopupAroundElement,
  positionPopupAtPoint: positionPopupAtPoint
};
});

;require.register("js/lib/selection", function(exports, require, module) {
var SelectionMgr,
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

SelectionMgr = (function(_super) {
  __extends(SelectionMgr, _super);

  SelectionMgr.include(Spine.Events);

  SelectionMgr.prototype._selections = [];

  function SelectionMgr() {
    SelectionMgr.__super__.constructor.apply(this, arguments);
  }

  SelectionMgr.prototype.setSelection = function(sel) {
    var value;
    this._selections = (function() {
      var _i, _len, _results;
      _results = [];
      for (_i = 0, _len = sel.length; _i < _len; _i++) {
        value = sel[_i];
        _results.push(value.clone());
      }
      return _results;
    })();
    return this.trigger('selectionChanged');
  };

  SelectionMgr.prototype.selectOnly = function(item) {
    this.removeAll();
    return this.selected(item);
  };

  SelectionMgr.prototype.toggleOnly = function(item) {
    var wasSelected;
    wasSelected = this.isSelected(item);
    this.removeAll();
    if (wasSelected) {
      this.trigger('selectionChanged');
      this.trigger("selectionChanged_" + item.cid, false);
    } else {
      this._selections.push(item);
      this.trigger('selectionChanged');
      this.trigger("selectionChanged_" + item.cid, true);
    }
    return false;
  };

  SelectionMgr.prototype.toggle = function(item) {
    var index;
    index = this._indexof(item);
    if (index > -1) {
      this._selections.splice(index, 1);
      this.trigger('selectionChanged');
      this.trigger("selectionChanged_" + item.cid, false);
    } else {
      this._selections.push(item);
      this.trigger('selectionChanged');
      this.trigger("selectionChanged_" + item.cid, true);
    }
    return false;
  };

  SelectionMgr.prototype.selected = function(item) {
    if (this._indexof(item) === -1) {
      this._selections.push(item);
      this.trigger('selectionChanged');
      this.trigger("selectionChanged_" + item.cid, true);
    }
    return false;
  };

  SelectionMgr.prototype.count = function() {
    return this._selections.length;
  };

  SelectionMgr.prototype.removeAll = function() {
    var lastIndex, _results;
    _results = [];
    while (lastIndex = this._selections.length) {
      _results.push(this.removed(this._selections[lastIndex - 1]));
    }
    return _results;
  };

  SelectionMgr.prototype.removed = function(item) {
    var index;
    index = this._indexof(item);
    if (index > -1) {
      this._selections.splice(index, 1);
      this.trigger('selectionChanged');
      this.trigger("selectionChanged_" + item.cid, false);
    }
    return false;
  };

  SelectionMgr.prototype.isSelected = function(item) {
    return this._indexof(item) > -1;
  };

  SelectionMgr.prototype.getSelection = function() {
    return this._selections;
  };

  SelectionMgr.prototype.hasSelection = function() {
    return this.count() > 0;
  };

  SelectionMgr.prototype._indexof = function(rec) {
    var i, r, _i, _len, _ref;
    _ref = this.getSelection();
    for (i = _i = 0, _len = _ref.length; _i < _len; i = ++_i) {
      r = _ref[i];
      if (rec.eql(r)) {
        return i;
      }
    }
    return -1;
  };

  return SelectionMgr;

})(Spine.Module);

module.exports = SelectionMgr;
});

;require.register("js/lib/session", function(exports, require, module) {
var Mac, Session,
  __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

Mac = require('js/lib/mac');

Session = (function(_super) {
  __extends(Session, _super);

  Session.include(Spine.Events);

  Session.include(Spine.Log);

  Session.prototype.logPrefix = '(Session)';

  Session.prototype.getScopeAccess = function(name) {};

  function Session() {
    this.doLogout = __bind(this.doLogout, this);
    var isAllowedAccess;
    Session.__super__.constructor.apply(this, arguments);
    isAllowedAccess = (function(_this) {
      return function(s, application) {
        var right, _i, _len, _ref;
        if (s.superUser) {
          return true;
        }
        _ref = application.accessRights;
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          right = _ref[_i];
          if (s.accessRights.map(function(r) {
            return r.name;
          }).contains(right.name)) {
            return true;
          }
        }
        return false;
      };
    })(this);
    setTimeout((function(_this) {
      return function() {
        return $.getJSON('/session').done(function(session) {
          if (session.error) {
            return console.log('Error loading session!');
          }
          if (session.suspended || session.changePasswordAtNextLogin) {
            return setTimeout(function() {
              return this.redirect('/');
            });
          }
          app.session = session;
          return app.getApps().done(function(apps) {
            var clbks, res, x, y, z, _i, _j, _k, _len, _len1, _len2;
            res = [];
            clbks = [];
            for (_i = 0, _len = apps.length; _i < _len; _i++) {
              x = apps[_i];
              if (isAllowedAccess(session, x)) {
                res.push("/api/v1/" + x.name + "/javascriptRoutes");
              }
            }
            for (_j = 0, _len1 = apps.length; _j < _len1; _j++) {
              y = apps[_j];
              if (!(isAllowedAccess(session, y))) {
                continue;
              }
              res.push("/" + y.name + "/assets/javascripts/" + y.name + "/tmpl.js");
              res.push("/" + y.name + "/assets/" + y.name + ".js");
              clbks["" + y.name + ".js"] = function() {
                return setTimeout(function() {
                  var Module;
                  Module = require("js/" + y.name);
                  return app.modules["" + y.name] = new Module({
                    app: app
                  });
                });
              };
            }
            for (_k = 0, _len2 = apps.length; _k < _len2; _k++) {
              z = apps[_k];
              if (isAllowedAccess(session, z)) {
                res.push("/" + z.name + "/assets/" + z.name + ".css");
              }
            }
            return setTimeout(function() {
              return yepnope({
                load: res,
                callback: clbks,
                complete: function() {
                  $.ajaxSetup({
                    beforeSend: function(xhr, req) {
                      var hdr;
                      if (req.url.search('/api/v1') === 0) {
                        hdr = Mac.createHeader(session, xhr, req);
                        xhr.setRequestHeader('Authorization', hdr);
                        req.url = "" + app.server + req.url;
                        req.crossDomain = true;
                        return req.xhrFields = {
                          withCredentials: true
                        };
                      }
                    }
                  });
                  setTimeout((function() {
                    return app.trigger('loaded');
                  }), 0);
                  return _this.setUpLoginStatusChecks();
                }
              }, 0);
            });
          });
        }).fail(function() {
          return console.log('No session found!');
        });
      };
    })(this));
  }

  Session.prototype.setUpLoginStatusChecks = function(interval) {
    var doCheckSession;
    if (interval == null) {
      interval = 30000;
    }
    doCheckSession = (function(_this) {
      return function() {
        return $.getJSON('/session').done(function(s) {
          _this.log("getLoginStatus success:" + arguments);
          if (s.error) {
            return app.trigger('session.error');
          }
          return app.trigger('session.loggedin', s);
        }).fail(function() {
          _this.log("getLoginStatus error:" + arguments);
          return app.trigger('session.error');
        });
      };
    })(this);
    doCheckSession();
    return this._refreshInterval = setInterval(doCheckSession, interval);
  };

  Session.prototype.doLogout = function() {
    this.log("doLogout");
    return $.getJSON('/api/v1/logout').done((function(_this) {
      return function() {
        _this.log("done doLogout:" + arguments);
        return _this.app.trigger('session.loggedout');
      };
    })(this));
  };

  return Session;

})(Spine.Module);

module.exports = Session;
});

;require.register("js/models/Tag", function(exports, require, module) {
var Tag,
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

Tag = (function(_super) {
  __extends(Tag, _super);

  function Tag() {
    return Tag.__super__.constructor.apply(this, arguments);
  }

  Tag.configure('Tag', 'name', 'color');

  Tag.Reload = function() {
    return $.Deferred((function(_this) {
      return function(deferred) {
        var d;
        d = $.Deferred();
        d.done(function(tags) {
          _this.refresh(tags, {
            clear: true
          });
          return deferred.resolve(_this.all());
        });
        return app.labels.getLabels().done(d.resolve);
      };
    })(this));
  };

  return Tag;

})(Spine.Model);

module.exports = Tag;
});

;
//# sourceMappingURL=main.js.map