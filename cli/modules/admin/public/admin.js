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
require.register("js/admin", function(exports, require, module) {
var Admin, Menu, TagM, Trash, User, UserM,
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; },
  __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };

UserM = require('js/models/admin/User');

TagM = require('js/models/Tag');

User = require('js/controllers/admin/User');

Trash = require('js/controllers/admin/Trash');

Menu = require('js/conjs/trollers/Menu');

Admin = (function(_super) {
  __extends(Admin, _super);

  Admin.prototype.USERS = 'users';

  Admin.prototype.TRASH = 'trash';

  Admin.prototype.ACCESS_RIGHTS = 'access_rights';

  Admin.prototype.USERS_CREATED_BY_ME = 'users.created_by_me';

  Admin.prototype.USERS_ACTIVE = 'users.active';

  Admin.prototype.USERS_SUSPENDED = 'users.suspended';

  Admin.prototype.USERS_SETTINGS = 'settings.users';

  Admin.prototype.USERS_STATS = 'stats.users';

  Admin.MenuMgr = (function(_super1) {
    __extends(MenuMgr, _super1);

    function MenuMgr() {
      MenuMgr.__super__.constructor.apply(this, arguments);
    }

    MenuMgr.prototype.delegate = function() {
      return {
        controller: new MenuMgr.S({
          mgr: this
        }),
        routes: this.routes
      };
    };

    MenuMgr.prototype.setButton = function(button) {
      this.button = button;
    };

    MenuMgr.prototype.routes = [
      {
        path: /^\/user/,
        callback: function() {
          this.admin.active();
          return app.modules['admin'].menu(app.modules['admin'].USERS);
        }
      }, {
        path: '/users-byme',
        callback: function() {
          this.admin.active();
          return app.modules['admin'].menu(app.modules['admin'].USERS_CREATED_BY_ME);
        }
      }, {
        path: '/users-active',
        callback: function() {
          this.admin.active();
          return app.modules['admin'].menu(app.modules['admin'].USERS_ACTIVE);
        }
      }, {
        path: '/users-suspended',
        callback: function() {
          this.admin.active();
          return app.modules['admin'].menu(app.modules['admin'].USERS_SUSPENDED);
        }
      }, {
        path: '/trash/users',
        callback: function() {
          this.admin.active();
          return app.modules['admin'].menu(app.modules['admin'].TRASH);
        }
      }, {
        path: '/settings/users',
        callback: function() {
          this.admin.active();
          return app.modules['admin'].menu(app.modules['admin'].USERS_SETTINGS);
        }
      }, {
        path: '/labelled/users/:label',
        callback: function(params) {
          this.admin.active();
          return app.modules['admin'].menu(decodeURIComponent(S(params.label).dasherize().chompLeft('-').s));
        }
      }
    ];

    MenuMgr.S = (function(_super2) {
      __extends(S, _super2);

      S.include(Spine.Log);

      S.prototype.logPrefix = '(Admin.Menu)';

      S.prototype.className = 'menu-admin';

      function S() {
        var key, value, _ref,
          _this = this;
        S.__super__.constructor.apply(this, arguments);
        if (this.mgr.button) {
          this.append(this.mgr.button);
        }
        _ref = this.mgr.menus();
        for (key in _ref) {
          value = _ref[key];
          this.append(value);
        }
        this.active(function() {
          return _this.log('Admin-Menu active');
        });
      }

      return S;

    })(Spine.Controller);

    return MenuMgr;

  }).call(this, Menu.Mgr);

  Admin.mgr = new Admin.MenuMgr;

  Admin.prototype.menu = function(id, clbk) {
    return Admin.mgr.activate(id, clbk);
  };

  Admin.prototype.users = require('js/lib/admin/users');

  Admin.labelTmpl = require('js/views/admin/menu/tag');

  Admin.stateTmpl = require('js/views/menu/link');

  Admin.prototype.defaultUrl = '/users';

  Admin.prototype.name = 'admin';

  function Admin() {
    var getUsersChildMenus, renderLabel, renderState,
      _this = this;
    Admin.__super__.constructor.apply(this, arguments);
    renderLabel = function(label) {
      return Admin.labelTmpl(label);
    };
    renderState = function(state) {
      var countClass;
      countClass = (function() {
        switch (state) {
          case this.USERS_CREATED_BY_ME:
            return 'byme';
          case this.USERS_ACTIVE:
            return 'active';
          case this.USERS_SUSPENDED:
            return 'suspended';
        }
      }).call(_this);
      return function(item) {
        item.countClass = "count-" + countClass;
        return Admin.stateTmpl(item);
      };
    };
    getUsersChildMenus = function() {
      return $.Deferred(function(deferred) {
        return app.on('loaded', function() {
          return TagM.Reload().done(function(objs) {
            var labels, obj;
            labels = (function() {
              var _i, _len, _results;
              _results = [];
              for (_i = 0, _len = objs.length; _i < _len; _i++) {
                obj = objs[_i];
                _results.push({
                  id: S(obj.name).dasherize().chompLeft('-').s,
                  title: S(obj.name).capitalize().s,
                  render: renderLabel,
                  href: "/labelled/users/" + (encodeURIComponent(obj.name))
                });
              }
              return _results;
            })();
            deferred.resolve([
              {
                id: _this.USERS_CREATED_BY_ME,
                title: 'Created by me',
                render: renderState(_this.USERS_CREATED_BY_ME),
                href: '/users-byme'
              }, {
                id: _this.USERS_ACTIVE,
                title: 'Active',
                render: renderState(_this.USERS_ACTIVE),
                href: '/users-active',
                state: 'users-active'
              }, {
                id: _this.USERS_SUSPENDED,
                title: 'Suspended',
                render: renderState(_this.USERS_SUSPENDED),
                href: '/users-suspended',
                state: 'suspended'
              }
            ].concat(labels));
            if (objs.length > 6) {
              return setTimeout(function() {
                jQuery('ul.children:not(:empty)').slimscroll({
                  height: '203px'
                });
                return jQuery('ul.children:not(:empty)').parent().siblings('.shadow').css({
                  opacity: 0.4
                });
              });
            }
          });
        });
      });
    };
    Admin.mgr.setButton(new Admin.NewButton);
    Admin.mgr.add({
      id: 'admin',
      header: 'ADMIN',
      items: [
        {
          id: this.USERS,
          title: 'Users',
          icon: 'glyphicon-user',
          href: '/users',
          getChildren: getUsersChildMenus
        }, {
          id: this.ACCESS_RIGHTS,
          title: 'Access Rights',
          icon: 'glyphicon-tags',
          href: '/roles'
        }, {
          id: this.TRASH,
          title: 'Trash',
          icon: 'glyphicon-trash',
          href: '/trash/users'
        }, {
          id: this.USERS_STATS,
          title: 'Stats',
          icon: 'glyphicon-stats',
          href: '/stats/users'
        }, {
          id: this.USERS_SETTINGS,
          title: 'Settings',
          icon: 'glyphicon-wrench',
          href: '/settings/users'
        }
      ]
    });
    this.app.registerApp({
      name: this.name,
      defaultUrl: this.defaultUrl,
      menu: Admin.mgr.delegate(),
      controllers: {
        users: User.Stack,
        trash: Trash.Stack
      },
      routes: {}
    });
    UserM.on('refresh', function() {
      jQuery('.count-byme').text(numeral(UserM.countUsersOf(app.session.user.id)).format());
      jQuery('.count-active').text(numeral(UserM.countActive()).format());
      return jQuery('.count-suspended').text(numeral(UserM.countSuspended()).format());
    });
    console.log('Loaded #admin module');
  }

  Admin.NewButton = (function(_super1) {
    __extends(NewButton, _super1);

    NewButton.tmpl = require('js/views/admin/menu/new')();

    NewButton.prototype.className = 'admin menu new-user';

    NewButton.prototype.events = {
      'click button.new-user': 'clicked'
    };

    function NewButton() {
      this.clicked = __bind(this.clicked, this);
      NewButton.__super__.constructor.apply(this, arguments);
      this.render();
    }

    NewButton.prototype.render = function() {
      return this.html(NewButton.tmpl);
    };

    NewButton.prototype.clicked = function(e) {
      e.preventDefault();
      e.stopPropagation();
      return this.navigate('/users/new');
    };

    return NewButton;

  })(Spine.Controller);

  return Admin;

}).call(this, Spine.Controller);

module.exports = Admin;
});

;require.register("js/controllers/admin/Trash", function(exports, require, module) {
var Menu, SelectionMgr, Trash, TrashM, position,
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; },
  __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };

TrashM = require('js/models/admin/DeletedUser');

Menu = require('js/controllers/Menu');

SelectionMgr = require('js/lib/selection');

position = require('js/lib/position');

Trash = (function() {
  function Trash() {}

  return Trash;

})();

Trash.Single = (function(_super) {
  __extends(Single, _super);

  function Single() {
    Single.__super__.constructor.apply(this, arguments);
  }

  return Single;

})(Spine.Controller);

Trash.List = (function(_super) {
  __extends(List, _super);

  List.prototype.logPrefix = '(Trash.List)';

  List.prototype.className = 'users-trash empty';

  List.prototype.calcHeight = function() {
    return $(window).height() - this.$('.selections').outerHeight() - this.toolbar.el.outerHeight() - $('.navbar').outerHeight() - 8;
  };

  List.SELECT_ALL = 'select:all';

  List.SELECT_NONE = 'select:none';

  List.SELECT_ACTIVE = 'select:active';

  List.SELECT_SUSPENDED = 'select:suspended';

  function List() {
    this.ToggleSelection = __bind(this.ToggleSelection, this);
    this.ToggleSelectionAll = __bind(this.ToggleSelectionAll, this);
    this.Prev = __bind(this.Prev, this);
    this.Next = __bind(this.Next, this);
    this.Reload = __bind(this.Reload, this);
    var _this = this;
    List.__super__.constructor.apply(this, arguments);
    this.selMgr = new SelectionMgr;
    this.selMgr.isWholePageSelected = function() {
      var row, _i, _len, _ref;
      _ref = _this.rows;
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        row = _ref[_i];
        if (!_this.selMgr.isSelected(row.user)) {
          return false;
        }
      }
      return true;
    };
    this.selMgr.isAllSelected = function() {
      var user, _i, _len, _ref;
      _ref = TrashM.all();
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        user = _ref[_i];
        if (!_this.selMgr.isSelected(user)) {
          return false;
        }
      }
      return true;
    };
    this.selMgr.on('toggle-selection', this.ToggleSelection);
    this.toolbar = new List.Toolbar({
      selMgr: this.selMgr,
      el: List.toolbarTmpl,
      mgr: this
    });
    this.list = new List.Body({
      selMgr: this.selMgr,
      mgr: this
    });
    this.contextmenu = new List.ContextMenu({
      el: List.contextmenuTmpl
    });
    this.render();
    this.active(function(state, label) {
      this.state = state;
      this.log("active State<" + this.state + ">");
      return this.delay(function() {
        app.modules['admin'].menu(app.modules['admin'].TRASH, function(item) {
          return item.loading();
        });
        TrashM.Reload();
        return this.delay(function() {
          return this.list.calcHeight();
        });
      });
    });
    this.toolbar.on('next', this.Next);
    this.toolbar.on('prev', this.Prev);
    this.toolbar.on('purge', function() {
      return _this.Purge(_this.selMgr.getSelection());
    });
    this.on('purge', function(user) {
      return _this.Purge([user]);
    });
    TrashM.on('refresh', function() {
      _this.Reload();
      return app.modules['admin'].menu(app.modules['admin'].TRASH, function(item) {
        return item.doneLoading();
      });
    });
    this.list.on('empty', function() {
      return _this.log('Empty trash');
    });
    this.contextmenu.on('purge', function(user) {
      return _this.Purge([user]);
    });
  }

  List.tmpl = require('js/views/admin/trash/list')({
    perPage: TrashM.MaxResults
  });

  List.rowTmpl = require('js/views/admin/trash/row')();

  List.toolbarTmpl = require('js/views/admin/trash/list.toolbar')();

  List.contextmenuTmpl = require('js/views/admin/trash/contextmenu')();

  List.prototype.start = 0;

  List.prototype.Reload = function() {
    return this.Refresh(TrashM.slice(this.start, this.start + TrashM.MaxResults));
  };

  List.prototype.Next = function() {
    this.start += TrashM.MaxResults;
    return this.Reload();
  };

  List.prototype.Prev = function() {
    if (this.start > 0) {
      this.start -= TrashM.MaxResults;
    }
    return this.Reload();
  };

  List.prototype.Purge = function(users) {
    return this.log("Purge [" + users + "]");
  };

  List.prototype.rows = [];

  List.prototype.add = function(row) {
    var index,
      _this = this;
    index = this.rows.push(row);
    row.on('release', function() {
      return _this.rows.splice(index - 1, 1);
    });
    row.on('contextmenu', function(evt, user) {
      return _this.contextmenu.Show(evt, user);
    });
    row.on('purge', function(user) {
      return _this.Purge([user]);
    });
    return row;
  };

  List._indexof = function(rec, all) {
    var i, r, _i, _len;
    if (all == null) {
      all = TrashM.all();
    }
    for (i = _i = 0, _len = all.length; _i < _len; i = ++_i) {
      r = all[i];
      if (rec.eql(r)) {
        return i;
      }
    }
    return -1;
  };

  List.prototype.AppendOne = function(user) {
    return this.AppendMany([user]);
  };

  List.prototype.AppendMany = function(users) {
    var all, delegate, user, _i, _len, _results;
    if (users.length) {
      this.el.removeClass('empty');
      all = TrashM.all();
      this.toolbar.page(List._indexof(users[0], all), List._indexof(users[users.length - 1], all), all.length);
      _results = [];
      for (_i = 0, _len = users.length; _i < _len; _i++) {
        user = users[_i];
        this.list.addUser(this.add(delegate = new List.Row({
          user: user,
          selMgr: this.selMgr,
          el: List.rowTmpl
        })).el);
        _results.push(delegate.DelegateEvents());
      }
      return _results;
    } else {
      this.el.addClass('empty');
      return this.toolbar.page(-1, -1, 0);
    }
  };

  List.prototype.Refresh = function(users) {
    var lastIndex;
    while (lastIndex = this.rows.length) {
      this.rows[lastIndex - 1].release();
    }
    return this.delay(function() {
      return this.AppendMany(users);
    });
  };

  List.prototype.ToggleSelectionAll = function(selectionState) {
    var user, _i, _j, _k, _len, _len1, _len2, _ref, _ref1, _ref2, _results, _results1, _results2;
    switch (selectionState) {
      case List.SELECT_ALL:
        _ref = TrashM.all();
        _results = [];
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          user = _ref[_i];
          _results.push(this.selMgr.selected(user));
        }
        return _results;
        break;
      case List.SELECT_NONE:
        this.selMgr.removeAll();
        return this.toolbar.allSelected = false;
      case List.SELECT_ACTIVE:
        _ref1 = TrashM.all();
        _results1 = [];
        for (_j = 0, _len1 = _ref1.length; _j < _len1; _j++) {
          user = _ref1[_j];
          if (!user.suspended) {
            _results1.push(this.selMgr.selected(user));
          } else {
            _results1.push(this.selMgr.removed(user));
          }
        }
        return _results1;
        break;
      case List.SELECT_SUSPENDED:
        _ref2 = TrashM.all();
        _results2 = [];
        for (_k = 0, _len2 = _ref2.length; _k < _len2; _k++) {
          user = _ref2[_k];
          if (user.suspended) {
            _results2.push(this.selMgr.selected(user));
          } else {
            _results2.push(this.selMgr.removed(user));
          }
        }
        return _results2;
    }
  };

  List.prototype.ToggleSelection = function(selectionState) {
    var lastIndex, row, selectedOnes, _i, _j, _k, _len, _len1, _len2, _ref, _ref1, _ref2, _results, _results1, _results2;
    switch (selectionState) {
      case List.SELECT_ALL:
        _ref = this.rows;
        _results = [];
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          row = _ref[_i];
          _results.push(this.selMgr.selected(row.user));
        }
        return _results;
        break;
      case List.SELECT_NONE:
        selectedOnes = this.selMgr.getSelection();
        while (lastIndex = selectedOnes.length) {
          this.selMgr.removed(selectedOnes[lastIndex - 1]);
        }
        this.toolbar.allSelected = false;
        this.delay(function() {
          return this.toolbar.selectionChanged();
        });
        return this.delay(function() {
          return this.list.selectionChanged();
        });
      case List.SELECT_ACTIVE:
        _ref1 = this.rows;
        _results1 = [];
        for (_j = 0, _len1 = _ref1.length; _j < _len1; _j++) {
          row = _ref1[_j];
          if (!row.user.suspended) {
            _results1.push(this.selMgr.selected(row.user));
          } else {
            _results1.push(this.selMgr.removed(row.user));
          }
        }
        return _results1;
        break;
      case List.SELECT_SUSPENDED:
        _ref2 = this.rows;
        _results2 = [];
        for (_k = 0, _len2 = _ref2.length; _k < _len2; _k++) {
          row = _ref2[_k];
          if (row.user.suspended) {
            _results2.push(this.selMgr.selected(row.user));
          } else {
            _results2.push(this.selMgr.removed(row.user));
          }
        }
        return _results2;
    }
  };

  List.prototype.render = function() {
    this.append(this.toolbar.el);
    this.append(this.list.el);
    return this.append(this.contextmenu.el);
  };

  List.Body = (function(_super1) {
    __extends(Body, _super1);

    Body.prototype.className = 'body';

    Body.elements = {
      '.list': 'list',
      '.scrollable': 'scrollable'
    };

    Body.events = {
      'click .empty': 'empty',
      'click .clear': 'clearAll',
      'click .select-all': 'selectAll'
    };

    Body.prototype.calcHeight = function() {
      return this.scrollable.css({
        height: this.mgr.calcHeight()
      });
    };

    function Body(_arg) {
      var _this = this;
      this.selMgr = _arg.selMgr;
      this.selectionChanged = __bind(this.selectionChanged, this);
      Body.__super__.constructor.apply(this, arguments);
      this.selMgr.on('selectionChanged', this.selectionChanged);
      this.on('page-selected', this.selectionChanged);
      this.on('clear-all', function() {
        _this.mgr.ToggleSelectionAll(List.SELECT_NONE);
        return _this.delay(function() {
          return this.calcHeight();
        });
      });
      this.on('select-all', function() {
        _this.mgr.ToggleSelectionAll(List.SELECT_ALL);
        return _this.delay(function() {
          return this.calcHeight();
        });
      });
      this.on('select', function(state) {
        _this.state = state;
        _this.mgr.ToggleSelection(state);
        return _this.delay(function() {
          return this.calcHeight();
        });
      });
      this.render();
      this.$(window).resize(this.calcHeight);
    }

    Body.prototype.selectionChanged = function() {
      this.el.toggleClass('page-selected', TrashM.count() > TrashM.MaxResults && this.selMgr.isWholePageSelected());
      this.el.toggleClass('all-selected', TrashM.count() > TrashM.MaxResults && this.selMgr.isAllSelected());
      return this.calcHeight();
    };

    Body.prototype.Off = function(fn) {
      this.selMgr.off('selectionChanged', this.selectionChanged);
      fn();
      return this.selMgr.on('selectionChanged', this.selectionChanged);
    };

    Body.prototype.render = function() {
      return this.html(List.tmpl);
    };

    Body.prototype.addUser = function(user) {
      return this.list.append(user);
    };

    Body.prototype.empty = function(e) {
      e.stopPropagation();
      e.preventDefault();
      return this.trigger('empty');
    };

    Body.prototype.clearAll = function(e) {
      var _this = this;
      e.stopPropagation();
      e.preventDefault();
      this.el.removeClass('all-selected');
      this.el.removeClass('page-selected');
      return this.Off(function() {
        return _this.trigger('clear-all');
      });
    };

    Body.prototype.selectAll = function(e) {
      var _this = this;
      e.stopPropagation();
      e.preventDefault();
      this.el.addClass('all-selected');
      this.el.removeClass('page-selected');
      return this.Off(function() {
        return _this.trigger('select-all');
      });
    };

    return Body;

  })(Spine.Controller);

  List.ContextMenu = (function(_super1) {
    __extends(ContextMenu, _super1);

    ContextMenu.prototype.logPrefix = '(Trash.ContextMenu)';

    ContextMenu.events = {
      'click .purge': 'purge',
      'click .restore': 'restore'
    };

    function ContextMenu() {
      var _this = this;
      ContextMenu.__super__.constructor.apply(this, arguments);
      this.$(document).click(function() {
        return _this.el.hide();
      });
    }

    ContextMenu.prototype.Show = function(evt, user) {
      this.user = user;
      position.positionPopupAtPoint(evt.clientX, evt.clientY, this.el[0]);
      return this.el.show();
    };

    ContextMenu.prototype.purge = function(e) {
      this.log("purge User<" + this.user.id + ">");
      e.preventDefault();
      return this.trigger('purge', this.user);
    };

    ContextMenu.prototype.restore = function(e) {
      this.log("restore User<" + this.user.id + ">");
      e.preventDefault();
      e.stopPropagation();
      return this.trigger('restore', this.user);
    };

    return ContextMenu;

  })(Spine.Controller);

  List.Row = (function(_super1) {
    __extends(Row, _super1);

    Row.Events = {
      'click': 'clicked',
      'click .purge': 'purge',
      'click .select': 'select',
      'contextmenu': 'contextmenu'
    };

    function Row() {
      this.selectionChanged = __bind(this.selectionChanged, this);
      Row.__super__.constructor.apply(this, arguments);
      this.selMgr.on("selectionChanged_" + this.user.cid, this.selectionChanged);
      this.listenTo(this.user, 'change', this.FillData);
      this.bind('release', function() {
        return this.selMgr.off("selectionChanged_" + this.user.cid, this.selectionChanged);
      });
      this.FillData();
    }

    Row.prototype.DelegateEvents = function() {
      return this.delegateEvents(Row.Events);
    };

    Row.prototype.FillData = function() {
      this.el.toggleClass('selected', this.selMgr.isSelected(this.user));
      this.$('.fullName').html(this.user.fullName());
      this.$('.primaryEmail').html(this.user.primaryEmail);
      return this.el;
    };

    Row.prototype.contextmenu = function(e) {
      e.stopPropagation();
      e.preventDefault();
      return this.trigger('contextmenu', e, this.user);
    };

    Row.prototype.selectionChanged = function(selected) {
      return this.el.toggleClass('selected', selected);
    };

    Row.prototype.clicked = function(evt) {
      evt.stopPropagation();
      return this.delay(function() {
        return this.navigate('/trash/user', this.user.id);
      });
    };

    Row.prototype.purge = function(evt) {
      evt.preventDefault();
      evt.stopPropagation();
      return this.trigger('purge', this.user.id);
    };

    Row.prototype.Off = function(fn) {
      this.selMgr.off("selectionChanged_" + this.user.cid, this.selectionChanged);
      fn();
      return this.selMgr.on("selectionChanged_" + this.user.cid, this.selectionChanged);
    };

    Row.prototype.select = function(evt) {
      var _this = this;
      evt.stopPropagation();
      this.Off(function() {
        if (_this.el.hasClass('selected')) {
          return _this.selMgr.removed(_this.user);
        } else {
          return _this.selMgr.selected(_this.user);
        }
      });
      return this.el.toggleClass('selected');
    };

    return Row;

  })(Spine.Controller);

  List.Toolbar = (function(_super1) {
    __extends(Toolbar, _super1);

    Toolbar.elements = {
      '.select': 'checkbox',
      '.next': 'next',
      '.prev': 'prev',
      '.select-toggle-wrapper': 'selectToggle'
    };

    Toolbar.Events = {
      'click .select': 'ToggleSelection',
      'click .restore': 'restore',
      'click .refresh': 'refresh',
      'click .purge': 'purge',
      'click .next': 'Next',
      'click .prev': 'Prev'
    };

    Toolbar.prototype.allSelected = false;

    Toolbar.SelectMenu = (function(_super2) {
      __extends(SelectMenu, _super2);

      SelectMenu.prototype.events = {
        'click a': 'select'
      };

      function SelectMenu() {
        SelectMenu.__super__.constructor.apply(this, arguments);
      }

      SelectMenu.prototype.select = function(e) {
        e.preventDefault();
        return this.trigger('select', this.$(e.target).attr('data-state'));
      };

      return SelectMenu;

    })(Spine.Controller);

    function Toolbar() {
      this.selectionChanged = __bind(this.selectionChanged, this);
      var _this = this;
      Toolbar.__super__.constructor.apply(this, arguments);
      this.selMgr.on('selectionChanged', this.selectionChanged);
      this.bind('release', function() {
        return this.selMgr.off('selectionChanged', this.selectionChanged);
      });
      this.delegateEvents(Toolbar.Events);
      (new Toolbar.SelectMenu({
        el: this.$('.selection-menu')
      })).on('select', function(state) {
        return _this.mgr.list.trigger('select', state);
      });
      this.selectToggle.dropdown().closest('#users-trash-select').on('show.bs.dropdown', function() {
        return _this.delay((function() {
          return this.selectToggle.closest('button.select').tooltip('hide');
        }), 10);
      });
    }

    Toolbar.prototype.selectionChanged = function() {
      var wholePage;
      this.el.toggleClass('has-selection', this.selMgr.hasSelection());
      this.el.toggleClass('all', wholePage = this.selMgr.isWholePageSelected());
      if (wholePage) {
        return this.allSelected = true;
      }
    };

    Toolbar.prototype.Off = function(fn) {
      this.selMgr.off('selectionChanged', this.selectionChanged);
      fn();
      return this.selMgr.on('selectionChanged', this.selectionChanged);
    };

    Toolbar.prototype.ToggleSelection = function() {
      var _this = this;
      this.el.toggleClass('has-selection', this.allSelected = !this.allSelected);
      this.el.toggleClass('all', this.allSelected);
      this.Off(function() {
        return _this.mgr.list.Off(function() {
          return _this.selMgr.trigger('toggle-selection', _this.allSelected ? List.SELECT_ALL : List.SELECT_NONE);
        });
      });
      return this.mgr.list.trigger('page-selected');
    };

    Toolbar.prototype.page = function(start, end, total) {
      if (total > TrashM.MaxResults) {
        this.next.prop('disabled', end === total - 1);
        this.prev.prop('disabled', start === 0);
        this.$('.start').html(start + 1);
        this.$('.end').html("" + (end + 1) + "&nbsp;");
        this.$('.count').html("" + (numeral(total).format()) + " &nbsp;");
      }
      this.el.toggleClass('needs-paging', total > TrashM.MaxResults);
      return this.el.toggleClass('empty', total === 0);
    };

    Toolbar.prototype.refresh = function() {
      if (this.selMgr.hasSelection()) {
        return;
      }
      return this.delay(function() {
        app.modules['admin'].menu(app.modules['admin'].TRASH, function(item) {
          return item.loading();
        });
        return TrashM.Reload();
      });
    };

    Toolbar.prototype.purge = function() {
      return this.log('List toolbar.purge');
    };

    Toolbar.prototype.restore = function() {
      return this.log('List toolbar.restore');
    };

    Toolbar.prototype.Next = function() {
      return this.trigger('next');
    };

    Toolbar.prototype.Prev = function() {
      return this.trigger('prev');
    };

    Toolbar.prototype.getSelectedUsers = function() {
      return this.selMgr.getSelection();
    };

    return Toolbar;

  })(Spine.Controller);

  return List;

}).call(this, Spine.Controller);

Trash.Stack = (function(_super) {
  __extends(Stack, _super);

  Stack.prototype.logPrefix = '(Trash.Stack)';

  Stack.prototype.className = 'spine stack users-trash';

  Stack.prototype.controllers = {
    list: Trash.List,
    single: Trash.Single
  };

  function Stack() {
    var _this = this;
    this.routes = {
      '/trash/users': 'list',
      '/trash/user/:id': function(params) {
        return _this.single.active(params.id);
      }
    };
    Stack.__super__.constructor.apply(this, arguments);
  }

  return Stack;

})(Spine.Stack);

module.exports = Trash;
});

;require.register("js/controllers/admin/User", function(exports, require, module) {
var LabelForm, Menu, SelectionMgr, TagM, User, UserM, UserTagM, position,
  __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

UserM = require('js/models/admin/User');

TagM = require('js/models/Tag');

UserTagM = require('js/models/admin/UserTag');

Menu = require('js/controllers/Menu');

LabelForm = require('js/controllers/LabelForm');

SelectionMgr = require('js/lib/selection');

position = require('js/lib/position');

User = (function() {
  function User() {}

  return User;

})();

User.Form = (function(_super) {
  __extends(Form, _super);

  Form.prototype.logPrefix = '(User.Form)';

  Form.prototype.className = 'user form spine stack';

  Form.defaultAvatarUrl = "http://www.gravatar.com/avatar/00000000000000000000000000000000?d=mm&f=y&s=195";

  Form.elements = {
    'fieldset': 'form',
    '.save': 'saveBtn'
  };

  function Form() {
    this.Save = __bind(this.Save, this);
    var _this = this;
    this.controllers = {
      edit: Form.Edit,
      New: Form.New
    };
    this.routes = {
      '/users/new': 'New',
      '/user/:id/edit': function(params) {
        return _this.edit.active(params.id);
      }
    };
    Form.__super__.constructor.apply(this, arguments);
    this.edit.Save = this.New.Save = this.Save;
    this.active(function() {
      return this.log("active");
    });
  }

  Form.toolbarTmpl = require('js/views/admin/user/form.toolbar');

  Form.UserInfo = function(it, id) {
    if (id == null) {
      id = void 0;
    }
    return {
      id: id,
      primaryEmail: app.cleaned(it.primaryEmail),
      givenName: app.cleaned(it.givenName),
      familyName: app.cleaned(it.familyName),
      gender: app.cleaned(it.gender),
      homeAddress: {
        city: app.cleaned(it.homeCity),
        country: app.cleaned(it.homeCountry),
        postalCode: app.cleaned(it.homePostalCode),
        streetAddress: app.cleaned(it.homeStreetAddress)
      },
      workAddress: {
        city: app.cleaned(it.workCity),
        country: app.cleaned(it.workCountry),
        postalCode: app.cleaned(it.workPostalCode),
        streetAddress: app.cleaned(it.workStreetAddress)
      },
      contacts: {
        mobiles: {
          mobile1: app.cleaned(it.mobile1),
          mobile2: app.cleaned(it.mobile2)
        },
        home: {
          email: app.cleaned(it.homeEmail),
          phoneNumber: app.cleaned(it.homePhoneNumber),
          fax: app.cleaned(it.homeFax)
        },
        work: {
          email: app.cleaned(it.workEmail),
          phoneNumber: app.cleaned(it.workPhoneNumber),
          fax: app.cleaned(it.workFax)
        }
      }
    };
  };

  Form.prototype.Save = function(user) {
    var done,
      _this = this;
    this.log("Save " + (JSON.stringify(user)));
    this.form.prop('disabled', true);
    this.saving();
    done = function() {
      _this.doneSaving();
      return _this.form.prop('disabled', false);
    };
    return done();
  };

  Form.prototype.saving = function() {
    this.saveBtn.button('saving');
    return this.el.addClass('saving');
  };

  Form.prototype.doneSaving = function() {
    this.el.removeClass('saving');
    return this.saveBtn.button('reset');
  };

  Form.Toolbar = (function(_super1) {
    __extends(Toolbar, _super1);

    Toolbar.events = {
      'click .back': 'cancel',
      'click .save': 'save'
    };

    function Toolbar() {
      Toolbar.__super__.constructor.apply(this, arguments);
    }

    Toolbar.prototype.cancel = function() {
      return this.navigate('/users');
    };

    Toolbar.prototype.save = function() {
      return this.trigger('save');
    };

    return Toolbar;

  })(Spine.Controller);

  Form.It = (function(_super1) {
    __extends(It, _super1);

    It.prototype.className = 'it scrollable';

    It.elements = {
      '[name="primaryEmail"]': 'primaryEmail',
      '[name="givenName"]': 'givenName',
      '[name="familyName"]': 'familyName',
      '[name="gender"]': 'gender',
      '[name="workAddress[city]"]': 'workCity',
      '[name="workAddress[country]"]': 'workCountry',
      '[name="workAddress[postalCode]"]': 'workPostalCode',
      '[name="workAddress[streetAddress]"]': 'workStreetAddress',
      '[name="homeAddress[city]"]': 'homeCity',
      '[name="homeAddress[country]"]': 'homeCountry',
      '[name="homeAddress[postalCode]"]': 'homePostalCode',
      '[name="homeAddress[streetAddress]"]': 'homeStreetAddress',
      '[name="contacts[mobiles][mobile1]"]': 'mobile1',
      '[name="contacts[mobiles][mobile2]"]': 'mobile2',
      '[name="contacts[work][phoneNumber]"]': 'workPhoneNumber',
      '[name="contacts[work][email]"]': 'workEmail',
      '[name="contacts[work][fax]"]': 'workFax',
      '[name="contacts[home][phoneNumber]"]': 'homePhoneNumber',
      '[name="contacts[home][email]"]': 'homeEmail',
      '[name="contacts[home][fax]"]': 'homeFax',
      '#avatar': 'avatar'
    };

    function It() {
      var _this = this;
      It.__super__.constructor.apply(this, arguments);
      this.avatarImg = new Image;
      this.avatarImg.onerror = function() {
        return _this.avatar[0].src = Form.defaultAvatarUrl;
      };
      this.avatarImg.onload = function() {
        return _this.avatar[0].src = _this.avatarImg.src;
      };
      this.render();
    }

    It.prototype.render = function() {
      var country, opts;
      this.html(this.tmpl);
      opts = ((function() {
        var _i, _len, _results;
        _results = [];
        for (_i = 0, _len = countries.length; _i < _len; _i++) {
          country = countries[_i];
          _results.push("<option value='" + country['code'] + "'>" + country['name'] + "</option>");
        }
        return _results;
      })()).join('');
      this.homeCountry.html(opts).chosen({
        allow_single_deselect: true,
        width: "330px"
      });
      this.workCountry.html(opts).chosen({
        allow_single_deselect: true,
        width: "330px"
      });
      this.$('.modules select').chosen({
        disable_search: true,
        width: '330px'
      });
      return this.el;
    };

    It.prototype.loadAvatar = function(id) {
      return this.delay(function() {
        return this.avatarImg.src = "/api/v1/avatar/" + id;
      });
    };

    It.prototype.Load = function(user) {
      var avatar, contacts, home, homeAddress, mobiles, work, workAddress;
      this.primaryEmail.val(user.primaryEmail);
      this.givenName.val(user.givenName);
      this.familyName.val(user.familyName);
      this.gender.val(user.gender);
      if (avatar = user.avatar) {
        this.loadAvatar(avatar);
      }
      if (homeAddress = user.homeAddress) {
        homeAddress.city && (this.homeCity.val(homeAddress.city));
        homeAddress.country && (this.homeCountry.val(homeAddress.country));
        homeAddress.postalCode && (this.homePostalCode.val(homeAddress.postalCode));
        homeAddress.streetAddress && (this.homeStreetAddress.val(homeAddress.streetAddress));
      }
      if (workAddress = user.workAddress) {
        workAddress.city && (this.workCity.val(workAddress.city));
        workAddress.country && (this.workCountry.val(workAddress.country));
        workAddress.postalCode && (this.workPostalCode.val(workAddress.postalCode));
        workAddress.streetAddress && (this.workStreetAddress.val(workAddress.streetAddress));
      }
      if (contacts = user.contacts) {
        if (mobiles = contacts.mobiles) {
          mobiles.mobile1 && (this.mobile1.val(mobiles.mobile1));
          mobiles.mobile2 && (this.mobile2.val(mobiles.mobile2));
        }
        if (home = contacts.home) {
          home.phoneNumber && (this.homePhoneNumber.val(home.phoneNumber));
          home.email && (this.homeEmail.val(home.email));
          home.fax && (this.homeFax.val(home.fax));
        }
        if (work = contacts.work) {
          work.phoneNumber && (this.workPhoneNumber.val(work.phoneNumber));
          work.email && (this.workEmail.val(work.email));
          return work.fax && (this.workFax.val(work.fax));
        }
      }
    };

    return It;

  })(Spine.Controller);

  Form.New = (function(_super1) {
    __extends(New, _super1);

    New.TITLE = 'Create a new user';

    New.prototype.className = 'user form new';

    New.prototype.tag = 'form';

    New.tmpl = require('js/views/admin/user/form.new')();

    New.prototype.calcHeight = function() {
      return $(window).height() - $('.selections').outerHeight() - this.toolbar.el.outerHeight() - $('.navbar').outerHeight() - 8;
    };

    function New() {
      this.doSave = __bind(this.doSave, this);
      var _this = this;
      New.__super__.constructor.apply(this, arguments);
      this.toolbar = new Form.Toolbar({
        el: Form.toolbarTmpl({
          title: New.TITLE
        })
      });
      this.it = new Form.It({
        tmpl: New.tmpl
      });
      this.$(window).resize(function() {
        return _this.it.el.css({
          height: _this.calcHeight()
        });
      });
      this.active(function() {
        this.log("New user");
        this.it.Load(UserM.Defaults);
        return this.delay(function() {
          return this.it.el.css({
            height: this.calcHeight()
          });
        });
      });
      this.toolbar.on('save', this.doSave);
      this.render();
    }

    New.prototype.render = function() {
      this.append(this.toolbar.el);
      return this.append(this.it.el);
    };

    New.prototype.doSave = function() {
      return this.Save(Form.UserInfo(this.it));
    };

    return New;

  })(Spine.Controller);

  Form.Edit = (function(_super1) {
    __extends(Edit, _super1);

    Edit.TITLE = 'Edit user';

    Edit.prototype.className = 'user form edit';

    Edit.prototype.tag = 'form';

    Edit.tmpl = require('js/views/admin/user/form.edit')();

    Edit.prototype.calcHeight = function() {
      return $(window).height() - $('.selections').outerHeight() - this.toolbar.el.outerHeight() - $('.navbar').outerHeight() - 8;
    };

    function Edit() {
      this.doSave = __bind(this.doSave, this);
      var _this = this;
      Edit.__super__.constructor.apply(this, arguments);
      this.toolbar = new Form.Toolbar({
        el: Form.toolbarTmpl({
          title: Edit.TITLE
        })
      });
      this.it = new Form.It({
        tmpl: Edit.tmpl
      });
      this.$(window).resize(function() {
        return _this.it.el.css({
          height: _this.calcHeight()
        });
      });
      this.active(function(id) {
        var ex, user;
        this.log("Edit user id=" + id);
        try {
          if (user = UserM.find(id)) {
            this.it.Load(user.toJSON());
            this.id = id;
          }
        } catch (_error) {
          ex = _error;
          this.log("Error finding User<" + id + ">");
        }
        return this.delay(function() {
          return this.it.el.css({
            height: this.calcHeight()
          });
        });
      });
      this.toolbar.on('save', this.doSave);
      this.render();
    }

    Edit.prototype.render = function() {
      this.append(this.toolbar.el);
      return this.append(this.it.el);
    };

    Edit.prototype.doSave = function() {
      return this.Save(Form.UserInfo(this.it, this.id));
    };

    return Edit;

  })(Spine.Controller);

  return Form;

}).call(this, Spine.Stack);

User.Single = (function(_super) {
  __extends(Single, _super);

  Single.prototype.logPrefix = '(User.Single)';

  Single.prototype.className = 'user single';

  function Single() {
    Single.__super__.constructor.apply(this, arguments);
    this.toolbar = new Single.Toolbar({
      el: Single.toolbarTmpl
    });
    this.it = new Single.It;
    this.active(function(id) {
      var ex, user;
      this.log("active User<" + id + ">");
      try {
        if (user = UserM.find(id)) {
          return this.it.Load(user);
        }
      } catch (_error) {
        ex = _error;
        return this.log("Error finding User#" + id);
      }
    });
  }

  Single.tmpl = require('js/views/admin/user/single')();

  Single.toolbarTmpl = require('js/views/admin/user/single.toolbar')();

  Single.prototype.render = function() {
    this.append(this.toolbar.el);
    return this.append(this.it.el);
  };

  Single.It = (function(_super1) {
    __extends(It, _super1);

    It.prototype.className = 'it scrollable';

    function It() {
      It.__super__.constructor.apply(this, arguments);
      this.render();
    }

    It.prototype.render = function() {
      return this.html(Single.tmpl);
    };

    It.prototype.Load = function(user) {
      if (this.user) {
        this.stopListening(this.user);
      }
      this.user = user;
      this.listenTo(this.user, 'change', this.FillData);
      return this.FillData();
    };

    It.prototype.FillData = function() {
      return this.el;
    };

    return It;

  })(Spine.Controller);

  Single.Toolbar = (function(_super1) {
    __extends(Toolbar, _super1);

    Toolbar.events = {
      'click .back': 'back',
      'click .refresh': 'refresh',
      'click .edit': 'edit',
      'click .purge': 'purge'
    };

    function Toolbar() {
      Toolbar.__super__.constructor.apply(this, arguments);
    }

    Toolbar.prototype.back = function() {
      var _this = this;
      return this.delay(function() {
        return _this.navigate('/users');
      });
    };

    Toolbar.prototype.refresh = function() {
      return this.log('Single toolbar.refresh');
    };

    Toolbar.prototype.edit = function() {
      return this.log('Single toolbar.edit');
    };

    Toolbar.prototype.purge = function() {
      return this.log('Single toolbar.purge');
    };

    return Toolbar;

  })(Spine.Controller);

  return Single;

}).call(this, Spine.Controller);

User.List = (function(_super) {
  __extends(List, _super);

  List.prototype.logPrefix = '(User.List)';

  List.prototype.className = 'users';

  List.prototype.calcHeight = function() {
    return $(window).height() - this.$('.selections').outerHeight() - this.toolbar.el.outerHeight() - $('.navbar').outerHeight() - 8;
  };

  List.ALL = 'all';

  List.OWN = 'byme';

  List.ACTIVE = 'active';

  List.SUSPENDED = 'suspended';

  List.LABELLED = 'labelled';

  List.SELECT_ALL = 'select:all';

  List.SELECT_NONE = 'select:none';

  List.SELECT_ACTIVE = 'select:active';

  List.SELECT_SUSPENDED = 'select:suspended';

  function List() {
    this.ToggleSelection = __bind(this.ToggleSelection, this);
    this.ToggleSelectionAll = __bind(this.ToggleSelectionAll, this);
    this.Prev = __bind(this.Prev, this);
    this.Next = __bind(this.Next, this);
    this.Reload = __bind(this.Reload, this);
    var _this = this;
    List.__super__.constructor.apply(this, arguments);
    this.selMgr = new SelectionMgr;
    this.selMgr.isWholePageSelected = function() {
      var row, _i, _len, _ref;
      _ref = _this.rows;
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        row = _ref[_i];
        if (!_this.selMgr.isSelected(row.user)) {
          return false;
        }
      }
      return true;
    };
    this.selMgr.isAllSelected = function() {
      var user, _i, _len, _ref;
      _ref = UserM.select(_this.getFilter());
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        user = _ref[_i];
        if (!_this.selMgr.isSelected(user)) {
          return false;
        }
      }
      return true;
    };
    this.selMgr.on('toggle-selection', this.ToggleSelection);
    this.toolbar = new List.Toolbar({
      selMgr: this.selMgr,
      el: List.toolbarTmpl,
      mgr: this
    });
    this.list = new List.Body({
      selMgr: this.selMgr,
      mgr: this
    });
    this.contextmenu = new List.ContextMenu({
      el: List.contextmenuTmpl
    });
    this.render();
    this.active(function(state, label) {
      this.state = state;
      this.log("active State<" + this.state + ">");
      this.label = decodeURIComponent(label);
      return this.delay(function() {
        switch (this.state) {
          case List.ALL:
            app.modules['admin'].menu(app.modules['admin'].USERS, function(item) {
              return item.loading();
            });
            break;
          case List.OWN:
            app.modules['admin'].menu(app.modules['admin'].USERS_CREATED_BY_ME, function(item) {
              return item.loading();
            });
            break;
          case List.ACTIVE:
            app.modules['admin'].menu(app.modules['admin'].USERS_ACTIVE, function(item) {
              return item.loading();
            });
            break;
          case List.SUSPENDED:
            app.modules['admin'].menu(app.modules['admin'].USERS_SUSPENDED, function(item) {
              return item.loading();
            });
            break;
          case List.LABELLED:
            app.modules['admin'].menu(S(this.label).dasherize().chompLeft('-').s, function(item) {
              return item.loading();
            });
        }
        UserM.Reload();
        return this.delay(function() {
          return this.list.calcHeight();
        });
      });
    });
    this.toolbar.on('next', this.Next);
    this.toolbar.on('prev', this.Prev);
    this.toolbar.on('purge', function() {
      return _this.Purge(_this.selMgr.getSelection());
    });
    UserM.on('refresh', function() {
      _this.Reload();
      switch (_this.state) {
        case List.ALL:
          return app.modules['admin'].menu(app.modules['admin'].USERS, function(item) {
            return item.doneLoading();
          });
        case List.OWN:
          return app.modules['admin'].menu(app.modules['admin'].USERS_CREATED_BY_ME, function(item) {
            return item.doneLoading();
          });
        case List.ACTIVE:
          return app.modules['admin'].menu(app.modules['admin'].USERS_ACTIVE, function(item) {
            return item.doneLoading();
          });
        case List.SUSPENDED:
          return app.modules['admin'].menu(app.modules['admin'].USERS_SUSPENDED, function(item) {
            return item.doneLoading();
          });
        case List.LABELLED:
          return app.modules['admin'].menu(S(_this.label).dasherize().chompLeft('-').s, function(item) {
            return item.doneLoading();
          });
      }
    });
    this.contextmenu.on('purge', function(user) {
      return _this.Purge([user]);
    });
  }

  List.tmpl = require('js/views/admin/user/list')({
    perPage: UserM.MaxResults
  });

  List.rowTmpl = require('js/views/admin/user/row')();

  List.toolbarTmpl = require('js/views/admin/user/list.toolbar')();

  List.contextmenuTmpl = require('js/views/admin/user/contextmenu')();

  List.prototype.start = 0;

  List.prototype.getFilter = function() {
    var label;
    switch (this.state) {
      case List.ALL:
        return function() {
          return true;
        };
      case List.OWN:
        return function(user) {
          return user.createdBy === app.session.user.id;
        };
      case List.ACTIVE:
        return function(user) {
          return !user.suspended;
        };
      case List.SUSPENDED:
        return function(user) {
          return user.suspended;
        };
      case List.LABELLED:
        label = this.label;
        return function(user) {
          return user.labels.indexOf(label) > -1;
        };
    }
  };

  List.prototype.Reload = function() {
    return this.Refresh(UserM.select(this.getFilter()).slice(this.start, this.start + UserM.MaxResults));
  };

  List.prototype.Next = function() {
    this.start += UserM.MaxResults;
    return this.Reload();
  };

  List.prototype.Prev = function() {
    if (this.start > 0) {
      this.start -= UserM.MaxResults;
    }
    return this.Reload();
  };

  List.prototype.Purge = function(users) {};

  List.prototype.rows = [];

  List.prototype.add = function(row) {
    var index,
      _this = this;
    index = this.rows.push(row);
    row.on('release', function() {
      return _this.rows.splice(index - 1, 1);
    });
    row.on('contextmenu', function(evt, user) {
      return _this.contextmenu.Show(evt, user);
    });
    return row;
  };

  List._indexof = function(rec, all) {
    var i, r, _i, _len;
    if (all == null) {
      all = UserM.select(this.getFilter());
    }
    for (i = _i = 0, _len = all.length; _i < _len; i = ++_i) {
      r = all[i];
      if (rec.eql(r)) {
        return i;
      }
    }
    return -1;
  };

  List.prototype.AppendOne = function(user) {
    return this.AppendMany([user]);
  };

  List.prototype.AppendMany = function(users) {
    var all, delegate, user, _i, _len, _results;
    if (users.length) {
      all = UserM.select(this.getFilter());
      this.toolbar.page(List._indexof(users[0], all), List._indexof(users[users.length - 1], all), all.length);
      _results = [];
      for (_i = 0, _len = users.length; _i < _len; _i++) {
        user = users[_i];
        this.list.addUser(this.add(delegate = new List.Row({
          user: user,
          selMgr: this.selMgr,
          el: List.rowTmpl
        })).el);
        _results.push(delegate.DelegateEvents());
      }
      return _results;
    } else {
      return this.toolbar.page(-1, -1, 0);
    }
  };

  List.prototype.Refresh = function(users) {
    var lastIndex;
    while (lastIndex = this.rows.length) {
      this.rows[lastIndex - 1].release();
    }
    return this.delay(function() {
      return this.AppendMany(users);
    });
  };

  List.prototype.ToggleSelectionAll = function(selectionState) {
    var user, _i, _j, _k, _len, _len1, _len2, _ref, _ref1, _ref2, _results, _results1, _results2;
    switch (selectionState) {
      case List.SELECT_ALL:
        _ref = UserM.select(this.getFilter());
        _results = [];
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          user = _ref[_i];
          _results.push(this.selMgr.selected(user));
        }
        return _results;
        break;
      case List.SELECT_NONE:
        this.selMgr.removeAll();
        return this.toolbar.allSelected = false;
      case List.SELECT_ACTIVE:
        _ref1 = UserM.select(this.getFilter());
        _results1 = [];
        for (_j = 0, _len1 = _ref1.length; _j < _len1; _j++) {
          user = _ref1[_j];
          if (!user.suspended) {
            _results1.push(this.selMgr.selected(user));
          } else {
            _results1.push(this.selMgr.removed(user));
          }
        }
        return _results1;
        break;
      case List.SELECT_SUSPENDED:
        _ref2 = UserM.select(this.getFilter());
        _results2 = [];
        for (_k = 0, _len2 = _ref2.length; _k < _len2; _k++) {
          user = _ref2[_k];
          if (user.suspended) {
            _results2.push(this.selMgr.selected(user));
          } else {
            _results2.push(this.selMgr.removed(user));
          }
        }
        return _results2;
    }
  };

  List.prototype.ToggleSelection = function(selectionState) {
    var lastIndex, row, selectedOnes, _i, _j, _k, _len, _len1, _len2, _ref, _ref1, _ref2, _results, _results1, _results2;
    switch (selectionState) {
      case List.SELECT_ALL:
        _ref = this.rows;
        _results = [];
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          row = _ref[_i];
          _results.push(this.selMgr.selected(row.user));
        }
        return _results;
        break;
      case List.SELECT_NONE:
        selectedOnes = this.selMgr.getSelection();
        while (lastIndex = selectedOnes.length) {
          this.selMgr.removed(selectedOnes[lastIndex - 1]);
        }
        this.toolbar.allSelected = false;
        this.delay(function() {
          return this.toolbar.selectionChanged();
        });
        return this.delay(function() {
          return this.list.selectionChanged();
        });
      case List.SELECT_ACTIVE:
        _ref1 = this.rows;
        _results1 = [];
        for (_j = 0, _len1 = _ref1.length; _j < _len1; _j++) {
          row = _ref1[_j];
          if (!row.user.suspended) {
            _results1.push(this.selMgr.selected(row.user));
          } else {
            _results1.push(this.selMgr.removed(row.user));
          }
        }
        return _results1;
        break;
      case List.SELECT_SUSPENDED:
        _ref2 = this.rows;
        _results2 = [];
        for (_k = 0, _len2 = _ref2.length; _k < _len2; _k++) {
          row = _ref2[_k];
          if (row.user.suspended) {
            _results2.push(this.selMgr.selected(row.user));
          } else {
            _results2.push(this.selMgr.removed(row.user));
          }
        }
        return _results2;
    }
  };

  List.prototype.render = function() {
    this.append(this.toolbar.el);
    this.append(this.list.el);
    return this.append(this.contextmenu.el);
  };

  List.Body = (function(_super1) {
    __extends(Body, _super1);

    Body.prototype.className = 'body';

    Body.elements = {
      '.list': 'list',
      '.scrollable': 'scrollable'
    };

    Body.events = {
      'click .clear': 'clearAll',
      'click .select-all': 'selectAll'
    };

    Body.prototype.calcHeight = function() {
      return this.scrollable.css({
        height: this.mgr.calcHeight()
      });
    };

    function Body() {
      this.Off = __bind(this.Off, this);
      this.selectionChanged = __bind(this.selectionChanged, this);
      this.calcHeight = __bind(this.calcHeight, this);
      var _this = this;
      Body.__super__.constructor.apply(this, arguments);
      this.selMgr.on('selectionChanged', this.selectionChanged);
      this.on('page-selected', this.selectionChanged);
      this.on('clear-all', function() {
        _this.mgr.ToggleSelectionAll(List.SELECT_NONE);
        return _this.delay(function() {
          return _this.calcHeight();
        });
      });
      this.on('select-all', function() {
        _this.mgr.ToggleSelectionAll(List.SELECT_ALL);
        return _this.delay(function() {
          return _this.calcHeight();
        });
      });
      this.on('select', function(state) {
        _this.state = state;
        _this.mgr.ToggleSelection(state);
        return _this.delay(function() {
          return _this.calcHeight();
        });
      });
      this.render();
      this.$(window).resize(function() {
        return _this.calcHeight();
      });
    }

    Body.prototype.selectionChanged = function() {
      var needMorThanOnePage;
      needMorThanOnePage = UserM.select(this.mgr.getFilter()).length > UserM.MaxResults;
      this.el.toggleClass('page-selected', needMorThanOnePage && this.selMgr.isWholePageSelected());
      this.el.toggleClass('all-selected', needMorThanOnePage && this.selMgr.isAllSelected());
      return this.calcHeight();
    };

    Body.prototype.Off = function(fn) {
      this.selMgr.off('selectionChanged', this.selectionChanged);
      fn();
      return this.selMgr.on('selectionChanged', this.selectionChanged);
    };

    Body.prototype.render = function() {
      return this.html(List.tmpl);
    };

    Body.prototype.addUser = function(user) {
      return this.list.append(user);
    };

    Body.prototype.clearAll = function(e) {
      var _this = this;
      e.stopPropagation();
      e.preventDefault();
      this.el.removeClass('all-selected');
      this.el.removeClass('page-selected');
      return this.Off(function() {
        return _this.trigger('clear-all');
      });
    };

    Body.prototype.selectAll = function(e) {
      var _this = this;
      e.stopPropagation();
      e.preventDefault();
      this.el.addClass('all-selected');
      this.el.removeClass('page-selected');
      return this.Off(function() {
        return _this.trigger('select-all');
      });
    };

    return Body;

  })(Spine.Controller);

  List.ContextMenu = (function(_super1) {
    __extends(ContextMenu, _super1);

    ContextMenu.prototype.logPrefix = '(User.ContextMenu)';

    ContextMenu.events = {
      'click .edit': 'edit',
      'click .purge': 'purge',
      'click .disable': 'suspend'
    };

    function ContextMenu() {
      var _this = this;
      ContextMenu.__super__.constructor.apply(this, arguments);
      this.$(document).click(function() {
        return _this.el.hide();
      });
    }

    ContextMenu.prototype.Show = function(evt, user) {
      this.user = user;
      position.positionPopupAtPoint(evt.clientX, evt.clientY, this.el[0]);
      return this.el.show();
    };

    ContextMenu.prototype.edit = function(e) {
      this.log("edit User<" + this.user.id + ">");
      e.preventDefault();
      this.delay(function() {
        return this.navigate('/user', this.user.id, 'edit');
      });
      return this.trigger('edit', this.user);
    };

    ContextMenu.prototype.purge = function(e) {
      this.log("purge User<" + this.user.id + ">");
      e.preventDefault();
      return this.trigger('purge', this.user);
    };

    ContextMenu.prototype.suspend = function(e) {
      this.log("suspend User<" + this.user.id + ">");
      e.preventDefault();
      return this.trigger('suspend', this.user);
    };

    return ContextMenu;

  })(Spine.Controller);

  List.Row = (function(_super1) {
    __extends(Row, _super1);

    Row.Events = {
      'click': 'clicked',
      'click .select': 'select',
      'contextmenu': 'contextmenu'
    };

    function Row() {
      this.selectionChanged = __bind(this.selectionChanged, this);
      var _this = this;
      Row.__super__.constructor.apply(this, arguments);
      this.selMgr.on("selectionChanged_" + this.user.cid, this.selectionChanged);
      this.listenTo(this.user, 'change', this.FillData);
      this.on('release', function() {
        return _this.selMgr.off("selectionChanged_" + _this.user.cid, _this.selectionChanged);
      });
      this.FillData();
    }

    Row.prototype.DelegateEvents = function() {
      return this.delegateEvents(Row.Events);
    };

    Row.prototype.FillData = function() {
      this.el.toggleClass('selected', this.selMgr.isSelected(this.user));
      this.$('.fullName').html(this.user.fullName());
      this.$('.primaryEmail').html(this.user.primaryEmail);
      return this.el;
    };

    Row.prototype.contextmenu = function(e) {
      e.stopPropagation();
      e.preventDefault();
      return this.trigger('contextmenu', e, this.user);
    };

    Row.prototype.selectionChanged = function(selected) {
      return this.el.toggleClass('selected', selected);
    };

    Row.prototype.clicked = function(evt) {
      evt.stopPropagation();
      return this.delay(function() {
        return this.navigate('/user', this.user.id);
      });
    };

    Row.prototype.Off = function(fn) {
      this.selMgr.off("selectionChanged_" + this.user.cid, this.selectionChanged);
      fn();
      return this.selMgr.on("selectionChanged_" + this.user.cid, this.selectionChanged);
    };

    Row.prototype.select = function(evt) {
      var _this = this;
      evt.stopPropagation();
      this.Off(function() {
        if (_this.el.hasClass('selected')) {
          return _this.selMgr.removed(_this.user);
        } else {
          return _this.selMgr.selected(_this.user);
        }
      });
      return this.el.toggleClass('selected');
    };

    return Row;

  })(Spine.Controller);

  List.Toolbar = (function(_super1) {
    __extends(Toolbar, _super1);

    Toolbar.elements = {
      '.select': 'checkbox',
      '.labels-menu': 'labelsElement',
      '.next': 'next',
      '.prev': 'prev',
      '.select-toggle-wrapper': 'selectToggle'
    };

    Toolbar.Events = {
      'click .select': 'ToggleSelection',
      'click .new': 'New',
      'click .refresh': 'refresh',
      'click .purge': 'purge',
      'click .next': 'Next',
      'click .prev': 'Prev'
    };

    Toolbar.prototype.allSelected = false;

    Toolbar.SelectMenu = (function(_super2) {
      __extends(SelectMenu, _super2);

      SelectMenu.prototype.events = {
        'click a': 'select'
      };

      function SelectMenu() {
        SelectMenu.__super__.constructor.apply(this, arguments);
      }

      SelectMenu.prototype.select = function(e) {
        e.preventDefault();
        return this.trigger('select', this.$(e.target).attr('data-state'));
      };

      return SelectMenu;

    })(Spine.Controller);

    function Toolbar() {
      this.selectionChanged = __bind(this.selectionChanged, this);
      var _this = this;
      Toolbar.__super__.constructor.apply(this, arguments);
      this.selMgr.on('selectionChanged', this.selectionChanged);
      this.bind('release', function() {
        return _this.selMgr.off('selectionChanged', _this.selectionChanged);
      });
      new Toolbar.Tags({
        el: this.labelsElement,
        mgr: this
      });
      this.delegateEvents(Toolbar.Events);
      (new Toolbar.SelectMenu({
        el: this.$('.selection-menu')
      })).on('select', function(state) {
        return _this.mgr.list.trigger('select', state);
      });
      this.selectToggle.dropdown().closest('#users-select').on('show.bs.dropdown', function() {
        return _this.delay((function() {
          return this.selectToggle.closest('button.select').tooltip('hide');
        }), 10);
      });
    }

    Toolbar.prototype.selectionChanged = function() {
      var wholePage;
      this.el.toggleClass('has-selection', this.selMgr.hasSelection());
      this.el.toggleClass('all', wholePage = this.selMgr.isWholePageSelected());
      if (wholePage) {
        return this.allSelected = true;
      }
    };

    Toolbar.prototype.Off = function(fn) {
      this.selMgr.off('selectionChanged', this.selectionChanged);
      fn();
      return this.selMgr.on('selectionChanged', this.selectionChanged);
    };

    Toolbar.prototype.ToggleSelection = function() {
      var _this = this;
      this.el.toggleClass('has-selection', this.allSelected = !this.allSelected);
      this.el.toggleClass('all', this.allSelected);
      this.Off(function() {
        return _this.mgr.list.Off(function() {
          return _this.selMgr.trigger('toggle-selection', _this.allSelected ? List.SELECT_ALL : List.SELECT_NONE);
        });
      });
      return this.mgr.list.trigger('page-selected');
    };

    Toolbar.prototype.page = function(start, end, total) {
      if (total > UserM.MaxResults) {
        this.next.prop('disabled', end === total - 1);
        this.prev.prop('disabled', start === 0);
        this.$('.start').html(start + 1);
        this.$('.end').html("" + (end + 1) + "&nbsp;");
        this.$('.count').html("" + (numeral(total).format()) + " &nbsp;");
      }
      this.el.toggleClass('needs-paging', total > UserM.MaxResults);
      return this.el.toggleClass('empty', total === 0);
    };

    Toolbar.prototype.New = function() {
      return this.delay(function() {
        return this.navigate('/users/new');
      });
    };

    Toolbar.prototype.refresh = function() {
      if (this.selMgr.hasSelection()) {
        return;
      }
      return this.delay(function() {
        switch (this.mgr.state) {
          case List.ALL:
            app.modules['admin'].menu(app.modules['admin'].USERS, function(item) {
              return item.loading();
            });
            break;
          case List.OWN:
            app.modules['admin'].menu(app.modules['admin'].USERS_CREATED_BY_ME, function(item) {
              return item.loading();
            });
            break;
          case List.ACTIVE:
            app.modules['admin'].menu(app.modules['admin'].USERS_ACTIVE, function(item) {
              return item.loading();
            });
            break;
          case List.SUSPENDED:
            app.modules['admin'].menu(app.modules['admin'].USERS_SUSPENDED, function(item) {
              return item.loading();
            });
            break;
          case List.LABELLED:
            app.modules['admin'].menu(app.modules['admin'].USERS, function(item) {
              return item.loading();
            });
        }
        return UserM.Reload();
      });
    };

    Toolbar.prototype.purge = function() {
      return this.log('List toolbar.purge');
    };

    Toolbar.prototype.Next = function() {
      return this.trigger('next');
    };

    Toolbar.prototype.Prev = function() {
      return this.trigger('prev');
    };

    Toolbar.prototype.getSelectedUsers = function() {
      return this.selMgr.getSelection();
    };

    Toolbar.Tags = (function(_super2) {
      __extends(Tags, _super2);

      Tags.elements = {
        '.labels-list': 'list',
        'input': 'search'
      };

      Tags.events = {
        'click .new-label': 'NewLabel',
        'click .manage-labels': 'MngLabels',
        'click .apply-labels': 'ApplyUserLabels',
        'click input': function(e) {
          return e.stopPropagation();
        },
        'input input': 'Refresh'
      };

      Tags.form = new LabelForm;

      Tags.labelTmpl = require('js/views/admin/user/label');

      Tags.Tag = (function(_super3) {
        __extends(Tag, _super3);

        Tag.events = {
          'click': 'ToggleUserLabel'
        };

        function Tag() {
          this.ToggleUserLabel = __bind(this.ToggleUserLabel, this);
          this.selectionChanged = __bind(this.selectionChanged, this);
          Tag.__super__.constructor.apply(this, arguments);
          this.selMgr.on("selectionChanged_" + this.label.cid, this.selectionChanged);
        }

        Tag.prototype.selectionChanged = function(isSelected) {
          return this.el.toggleClass('selected', isSelected);
        };

        Tag.prototype.ToggleUserLabel = function(e) {
          e.preventDefault();
          e.stopPropagation();
          return this.selMgr.toggle(this.label);
        };

        return Tag;

      })(Spine.Controller);

      function Tags() {
        this.onShow = __bind(this.onShow, this);
        this.getSelectionMap = __bind(this.getSelectionMap, this);
        this.getSelection = __bind(this.getSelection, this);
        this.Refresh = __bind(this.Refresh, this);
        var _this = this;
        Tags.__super__.constructor.apply(this, arguments);
        this.selMgr = new SelectionMgr;
        this.el.on('show.bs.dropdown', this.onShow);
        Tags.form.on('save', this.SaveTag);
        this.selMgr.on('selectionChanged', function() {
          return _this.el.toggleClass('labels-selected', !app.arraysEqual(_this.selMgr.getSelection(), _this.initiallySelectedLabels));
        });
      }

      Tags.prototype.Refresh = function() {
        var label, lastIndex, name, q, _i, _j, _len, _len1, _ref, _ref1;
        while (lastIndex = this.labels.length) {
          this.labels[lastIndex - 1].release();
        }
        q = app.cleaned(this.search);
        if (q === '') {
          _ref = TagM.all();
          for (_i = 0, _len = _ref.length; _i < _len; _i++) {
            label = _ref[_i];
            this.list.append(this.add(new Tags.Tag({
              label: label,
              selMgr: this.selMgr,
              el: Tags.labelTmpl({
                name: label.name
              })
            })).el);
          }
        } else {
          _ref1 = TagM.all();
          for (_j = 0, _len1 = _ref1.length; _j < _len1; _j++) {
            label = _ref1[_j];
            if (!(label.name.match(new RegExp("^(.*)(" + q + ")(.*)$")))) {
              continue;
            }
            name = label.name.replace(new RegExp("^(.*)(" + q + ")(.*)$"), function(name, $1, $2, $3) {
              if (!!$2) {
                return "" + $1 + "<cite class='q'>" + $2 + "</cite>" + $3;
              } else {
                return name;
              }
            });
            this.list.append(this.add(new Tags.Tag({
              label: label,
              selMgr: this.selMgr,
              el: Tags.labelTmpl({
                name: name
              })
            })).el);
          }
        }
        return this.delay(function() {
          return this.selMgr.setSelection(this.initiallySelectedLabels = this.getSelection());
        });
      };

      Tags.prototype.getSelection = function() {
        var labels, selectedLabels, _, _ref;
        selectedLabels = [];
        _ref = this.getSelectionMap();
        for (_ in _ref) {
          labels = _ref[_];
          selectedLabels = [].concat.apply(selectedLabels, labels);
        }
        return selectedLabels;
      };

      Tags.prototype.getSelectionMap = function() {
        var id, selectedUsers, selectionMap, user, _i, _len;
        selectionMap = {};
        selectedUsers = (function() {
          var _i, _len, _ref, _results;
          _ref = this.mgr.getSelectedUsers();
          _results = [];
          for (_i = 0, _len = _ref.length; _i < _len; _i++) {
            user = _ref[_i];
            _results.push(user.id);
          }
          return _results;
        }).call(this);
        for (_i = 0, _len = selectedUsers.length; _i < _len; _i++) {
          id = selectedUsers[_i];
          selectionMap[id] || (selectionMap[id] = []);
          selectionMap[id].push(UserTagM.getUserTags(id));
        }
        return selectionMap;
      };

      Tags.prototype.labels = [];

      Tags.prototype.add = function(label) {
        var index,
          _this = this;
        index = this.labels.push(label);
        label.on('release', function() {
          return _this.labels.splice(index - 1, 1);
        });
        return label;
      };

      Tags.prototype.onShow = function() {
        return TagM.Reload().done(this.Refresh);
      };

      Tags.prototype.NewLabel = function(e) {
        e.preventDefault();
        return this.delay(function() {
          return Tags.form.New();
        });
      };

      Tags.prototype.MngLabels = function(e) {
        e.preventDefault();
        return this.delay(function() {
          return this.navigate('/labels');
        });
      };

      Tags.prototype.ApplyUserLabels = function(e) {
        return e.preventDefault();
      };

      Tags.prototype.SaveTag = function(info) {};

      return Tags;

    }).call(this, Spine.Controller);

    return Toolbar;

  }).call(this, Spine.Controller);

  return List;

}).call(this, Spine.Controller);

User.Stack = (function(_super) {
  __extends(Stack, _super);

  Stack.prototype.logPrefix = '(User.Stack)';

  Stack.prototype.className = 'spine stack users';

  Stack.prototype.controllers = {
    list: User.List,
    single: User.Single,
    form: User.Form
  };

  function Stack() {
    var _this = this;
    this.routes = {
      '/users': function() {
        return _this.list.active(User.List.ALL);
      },
      '/labelled/users/:label': function(params) {
        return _this.list.active(User.List.LABELLED, params.label);
      },
      '/users-:state': function(params) {
        return _this.list.active(params.state);
      },
      '/user/:id': function(params) {
        return _this.single.active(params.id);
      }
    };
    Stack.__super__.constructor.apply(this, arguments);
  }

  return Stack;

})(Spine.Stack);

module.exports = User;
});

;require.register("js/lib/admin/users", function(exports, require, module) {
var R, users;

R = adminRoutes.controllers.admin.Users;

users = (function() {
  function users() {}

  users.getUser = function(id) {
    return $.getJSON(R.getUser(id).url);
  };

  users.getUsersStats = function() {
    return $.getJSON(R.getUsersStats().url);
  };

  users.getUsers = function(page) {
    if (page == null) {
      page = 0;
    }
    return $.getJSON(R.getUsers(page).url);
  };

  users.getUserRoles = function(id) {
    return $.getJSON(R.getUserRoles(id).url);
  };

  users.saveUser = function(spec) {
    var route;
    route = R.addUser();
    return $.ajax({
      type: route.type,
      url: route.url,
      dataType: 'json',
      contentType: 'application/json; charset=UTF-8',
      data: JSON.stringify(spec)
    });
  };

  users.updateUser = function(id, spec) {
    var route;
    route = R.updateUser(id);
    return $.ajax({
      type: route.type,
      url: route.url,
      dataType: 'json',
      contentType: 'application/json; charset=UTF-8',
      data: JSON.stringify(spec)
    });
  };

  users.removeUsers = function(users) {
    var route;
    route = R.deleteUsers(users);
    return $.ajax({
      type: route.type,
      url: route.url,
      dataType: 'json'
    });
  };

  users.purgeUsers = function(users) {
    var route;
    route = R.purgeUsers(users);
    return $.ajax({
      type: route.type,
      url: route.url,
      dataType: 'json'
    });
  };

  users.undeleteUsers = function(users) {
    var route;
    route = R.undeleteUsers(users);
    return $.ajax({
      type: route.type,
      url: route.url,
      dataType: 'json'
    });
  };

  users.changePasswd = function(id, newPasswd, oldPassword) {
    var route;
    route = R.changePasswd(id);
    return $.ajax({
      type: route.type,
      url: route.url,
      dataType: 'json',
      contentType: 'application/json; charset=UTF-8',
      data: JSON.stringify({
        password: newPasswd,
        old_password: oldPassword
      })
    });
  };

  users.setAddress = function(id, spec) {
    var route;
    route = R.updateUser(id);
    return $.ajax({
      type: route.type,
      url: route.url,
      dataType: 'json',
      contentType: 'application/json; charset=UTF-8',
      data: JSON.stringify(spec)
    });
  };

  users.remHomeAddress = function(id) {
    var route;
    route = R.updateUser(id);
    return $.ajax({
      type: route.type,
      url: route.url,
      dataType: 'json',
      contentType: 'application/json; charset=UTF-8',
      data: JSON.stringify({
        homeAddress: {}
      })
    });
  };

  users.remWorkAddress = function(id) {
    var route;
    route = R.updateUser(id);
    return $.ajax({
      type: route.type,
      url: route.url,
      dataType: 'json',
      contentType: 'application/json; charset=UTF-8',
      data: JSON.stringify({
        workAddress: {}
      })
    });
  };

  users.updateContacts = function(id, contacts) {
    var route;
    route = R.updateUser(id);
    return $.ajax({
      type: route.type,
      url: route.url,
      contentType: 'application/json; charset=UTF-8',
      dataType: 'json',
      data: JSON.stringify({
        contacts: contacts
      })
    });
  };

  users.grantRoles = function(id, roles) {
    var route;
    route = R.grantUserRoles(id, roles);
    return $.ajax({
      type: route.type,
      url: route.url,
      dataType: 'json'
    });
  };

  users.revokeRoles = function(id, roles) {
    var route;
    route = R.revokeUserRoles(id, roles);
    return $.ajax({
      type: route.type,
      url: route.url,
      dataType: 'json'
    });
  };

  users.purgeAvatar = function(id, avatarId) {
    var route;
    route = R.purgeAvatar(id, avatarId);
    return $.ajax({
      type: route.type,
      url: route.url,
      dataType: 'json'
    });
  };

  users.downloadAvatar = function(avatarId) {
    return $.getJSON(R.downloadAvatar(avatarId).url);
  };

  users.uploadAvatar = function(id, file) {
    var sendFile, type, url, _ref;
    sendFile = function(_arg) {
      var file, type, url;
      type = _arg.type, url = _arg.url, file = _arg.file;
      return $.ajax({
        type: type,
        url: url,
        data: file,
        success: function() {
          return console.log("success");
        },
        xhrFields: {
          onprogress: function(progress) {
            var percentage;
            percentage = Math.floor((progress.total / progress.totalSize) * 100);
            console.log('progress', percentage);
            if (percentage === 100) {
              return console.log('DONE!');
            }
          }
        },
        processData: false,
        contentType: file.type,
        dataType: 'json'
      });
    };
    _ref = R.uploadAvatar(id, file.name), type = _ref.type, url = _ref.url;
    return sendFile({
      type: type,
      url: url,
      file: file
    });
  };

  users.getUserRoles = function(id) {
    return $.getJSON(R.getUserRoles(id).url);
  };

  users.userExists = function(username) {
    return $.getJSON(R.userExists(username).url);
  };

  users.getUsersInTrash = function() {
    return $.getJSON(R.getPurgedUsers().url);
  };

  users.labelUser = function(id, labels) {
    var route;
    route = R.addUserTags(id, labels);
    return $.ajax({
      type: route.type,
      url: route.url,
      dataType: 'json'
    });
  };

  users.unLabelUser = function(id, labels) {
    var route;
    route = R.purgeUserTags(id, labels);
    return $.ajax({
      type: route.type,
      url: route.url,
      dataType: 'json'
    });
  };

  users.getUserLabels = function(id) {
    return $.getJSON(R.getUserTags(id).url);
  };

  return users;

})();

module.exports = users;
});

;require.register("js/models/admin/DeletedUser", function(exports, require, module) {
var User, _ref,
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

User = (function(_super) {
  __extends(User, _super);

  function User() {
    _ref = User.__super__.constructor.apply(this, arguments);
    return _ref;
  }

  User.MaxResults = 50;

  User.configure('User', 'id', 'primaryEmail', 'givenName', 'familyName', 'gender', 'homeAddress', 'workAddress', 'contacts', 'avatar', 'lastLoginTime', 'createdAt', 'createdBy', 'lastModifiedAt', 'lastModifiedBy', 'suspended');

  User.prototype.fullName = function() {
    return "" + this.givenName + " " + this.familyName;
  };

  User.Reload = function() {
    var _this = this;
    return $.Deferred(function(deferred) {
      var d;
      d = $.Deferred();
      d.done(function(users) {
        _this.refresh(users, {
          clear: true
        });
        return deferred.resolve(_this.all());
      });
      return app.modules['admin'].users.getUsersInTrash().done(d.resolve);
    });
  };

  return User;

})(Spine.Model);

module.exports = User;
});

;require.register("js/models/admin/User", function(exports, require, module) {
var User, _ref,
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

User = (function(_super) {
  __extends(User, _super);

  function User() {
    _ref = User.__super__.constructor.apply(this, arguments);
    return _ref;
  }

  User.MaxResults = 50;

  User.configure('User', 'id', 'primaryEmail', 'givenName', 'familyName', 'gender', 'homeAddress', 'workAddress', 'contacts', 'avatar', 'lastLoginTime', 'createdAt', 'createdBy', 'lastModifiedAt', 'lastModifiedBy', 'suspended');

  User.comparator = function(a, b) {
    var res;
    if (a.lastLoginTime) {
      if (b.lastLoginTime) {
        if ((res = a.lastLoginTime - b.lastLoginTime) === 0) {
          if (a.lastModifiedAt) {
            if (b.lastModifiedAt) {
              return a.lastModifiedAt - b.lastModifiedAt;
            }
            return 1;
          }
          if (b.lastModifiedAt) {
            return 1;
          }
          return a.createdAt - b.createdAt;
        } else {
          return res;
        }
      }
      return 1;
    }
    if (b.lastLoginTime) {
      return 1;
    }
    if (a.lastModifiedAt) {
      if (b.lastModifiedAt) {
        if ((res = a.lastModifiedAt - b.lastModifiedAt) === 0) {
          return a.createdAt - b.createdAt;
        } else {
          return res;
        }
      }
      return 1;
    }
    if (b.lastModifiedAt) {
      return 1;
    }
    return a.createdAt - b.createdAt;
  };

  User.prototype.fullName = function() {
    return "" + this.givenName + " " + this.familyName;
  };

  User.Reload = function() {
    var _this = this;
    return $.Deferred(function(d) {
      var completed, users;
      users = [];
      completed = 0;
      return app.modules['admin'].users.getUsersStats().done(function(_arg) {
        var PAGES, count;
        count = _arg['count'];
        PAGES = Math.ceil(count / _this.MaxResults);
        if (PAGES === 0) {
          _this.refresh([], {
            clear: true
          });
          d.resolve(_this.all());
          return;
        }
        return $.Deferred(function(deferred) {
          var LoadPage, i, _i, _results;
          deferred.progress(function(us) {
            users = [].concat.apply(users, us);
            if (++completed === PAGES) {
              return deferred.resolve(users);
            }
          });
          deferred.done(function(us) {
            return _this.refresh(us, {
              clear: true
            });
          });
          LoadPage = function(index) {
            return app.modules['admin'].users.getUsers(index).done(deferred.notify);
          };
          _results = [];
          for (i = _i = 0; _i < PAGES; i = _i += 1) {
            _results.push(LoadPage(i));
          }
          return _results;
        });
      });
    });
  };

  User.countUsersOf = function(id) {
    var byHim;
    byHim = function() {
      return User.select(function(user) {
        return user.createdBy === id;
      });
    };
    return byHim().length;
  };

  User.countSuspended = function() {
    var suspended;
    suspended = function() {
      return User.select(function(user) {
        return user.suspended;
      });
    };
    return suspended().length;
  };

  User.countActive = function() {
    var active;
    active = function() {
      return User.select(function(user) {
        return !user.suspended;
      });
    };
    return active().length;
  };

  User.Defaults = {
    primaryEmail: '',
    givenName: '',
    familyName: '',
    gender: 'Male',
    homeAddress: {
      city: window['sGeobytesCity'],
      country: window['sGeobytesIso2']
    },
    workAddress: {
      city: window['sGeobytesCity'],
      country: window['sGeobytesIso2']
    }
  };

  return User;

}).call(this, Spine.Model);

module.exports = User;
});

;require.register("js/models/admin/UserTag", function(exports, require, module) {
var UserTag, _ref,
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

UserTag = require('models/admin/UserTag');

UserTag = (function(_super) {
  __extends(UserTag, _super);

  function UserTag() {
    _ref = UserTag.__super__.constructor.apply(this, arguments);
    return _ref;
  }

  UserTag.configure('UserTag', 'label', 'userId');

  UserTag.Reload = function() {
    var _this = this;
    return $.Deferred(function(deferred) {
      var d;
      d = $.Deferred();
      d.done(function(userTags) {
        _this.refresh(userTags, {
          clear: true
        });
        return deferred.resolve(_this.all());
      });
      return app.labels.getLabels().done(d.resolve);
    });
  };

  UserTag.getUserTags = function(userId) {
    var _this = this;
    return UserTag.select(function(userTag) {
      return userTag.userId === userId;
    });
  };

  return UserTag;

})(Spine.Model);

module.exports = UserTag;
});

;!function(e){if("object"==typeof exports)module.exports=e();else if("function"==typeof define&&define.amd)define(e);else{var f;"undefined"!=typeof window?f=window:"undefined"!=typeof global?f=global:"undefined"!=typeof self&&(f=self),f.jade=e()}}(function(){var define,module,exports;return (function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);throw new Error("Cannot find module '"+o+"'")}var f=n[o]={exports:{}};t[o][0].call(f.exports,function(e){var n=t[o][1][e];return s(n?n:e)},f,f.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
'use strict';

/**
 * Merge two attribute objects giving precedence
 * to values in object `b`. Classes are special-cased
 * allowing for arrays and merging/joining appropriately
 * resulting in a string.
 *
 * @param {Object} a
 * @param {Object} b
 * @return {Object} a
 * @api private
 */

exports.merge = function merge(a, b) {
  if (arguments.length === 1) {
    var attrs = a[0];
    for (var i = 1; i < a.length; i++) {
      attrs = merge(attrs, a[i]);
    }
    return attrs;
  }
  var ac = a['class'];
  var bc = b['class'];

  if (ac || bc) {
    ac = ac || [];
    bc = bc || [];
    if (!Array.isArray(ac)) ac = [ac];
    if (!Array.isArray(bc)) bc = [bc];
    a['class'] = ac.concat(bc).filter(nulls);
  }

  for (var key in b) {
    if (key != 'class') {
      a[key] = b[key];
    }
  }

  return a;
};

/**
 * Filter null `val`s.
 *
 * @param {*} val
 * @return {Boolean}
 * @api private
 */

function nulls(val) {
  return val != null && val !== '';
}

/**
 * join array as classes.
 *
 * @param {*} val
 * @return {String}
 */
exports.joinClasses = joinClasses;
function joinClasses(val) {
  return Array.isArray(val) ? val.map(joinClasses).filter(nulls).join(' ') : val;
}

/**
 * Render the given classes.
 *
 * @param {Array} classes
 * @param {Array.<Boolean>} escaped
 * @return {String}
 */
exports.cls = function cls(classes, escaped) {
  var buf = [];
  for (var i = 0; i < classes.length; i++) {
    if (escaped && escaped[i]) {
      buf.push(exports.escape(joinClasses([classes[i]])));
    } else {
      buf.push(joinClasses(classes[i]));
    }
  }
  var text = joinClasses(buf);
  if (text.length) {
    return ' class="' + text + '"';
  } else {
    return '';
  }
};

/**
 * Render the given attribute.
 *
 * @param {String} key
 * @param {String} val
 * @param {Boolean} escaped
 * @param {Boolean} terse
 * @return {String}
 */
exports.attr = function attr(key, val, escaped, terse) {
  if ('boolean' == typeof val || null == val) {
    if (val) {
      return ' ' + (terse ? key : key + '="' + key + '"');
    } else {
      return '';
    }
  } else if (0 == key.indexOf('data') && 'string' != typeof val) {
    return ' ' + key + "='" + JSON.stringify(val).replace(/'/g, '&apos;') + "'";
  } else if (escaped) {
    return ' ' + key + '="' + exports.escape(val) + '"';
  } else {
    return ' ' + key + '="' + val + '"';
  }
};

/**
 * Render the given attributes object.
 *
 * @param {Object} obj
 * @param {Object} escaped
 * @return {String}
 */
exports.attrs = function attrs(obj, terse){
  var buf = [];

  var keys = Object.keys(obj);

  if (keys.length) {
    for (var i = 0; i < keys.length; ++i) {
      var key = keys[i]
        , val = obj[key];

      if ('class' == key) {
        if (val = joinClasses(val)) {
          buf.push(' ' + key + '="' + val + '"');
        }
      } else {
        buf.push(exports.attr(key, val, false, terse));
      }
    }
  }

  return buf.join('');
};

/**
 * Escape the given string of `html`.
 *
 * @param {String} html
 * @return {String}
 * @api private
 */

exports.escape = function escape(html){
  var result = String(html)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
  if (result === '' + html) return html;
  else return result;
};

/**
 * Re-throw the given `err` in context to the
 * the jade in `filename` at the given `lineno`.
 *
 * @param {Error} err
 * @param {String} filename
 * @param {String} lineno
 * @api private
 */

exports.rethrow = function rethrow(err, filename, lineno, str){
  if (!(err instanceof Error)) throw err;
  if ((typeof window != 'undefined' || !filename) && !str) {
    err.message += ' on line ' + lineno;
    throw err;
  }
  try {
    str =  str || require('fs').readFileSync(filename, 'utf8')
  } catch (ex) {
    rethrow(err, null, lineno)
  }
  var context = 3
    , lines = str.split('\n')
    , start = Math.max(lineno - context, 0)
    , end = Math.min(lines.length, lineno + context);

  // Error context
  var context = lines.slice(start, end).map(function(line, i){
    var curr = i + start + 1;
    return (curr == lineno ? '  > ' : '    ')
      + curr
      + '| '
      + line;
  }).join('\n');

  // Alter exception message
  err.path = filename;
  err.message = (filename || 'Jade') + ':' + lineno
    + '\n' + context + '\n\n' + err.message;
  throw err;
};

},{"fs":2}],2:[function(require,module,exports){

},{}]},{},[1])
(1)
});
;
//# sourceMappingURL=admin.js.map