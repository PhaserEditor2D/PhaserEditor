function parseFloat(string) { return 0; }

function ReferenceError() {
}
ReferenceError.prototype = new Error();

function URIError() {
}
URIError.prototype = new Error();

function _JSON() {
}
_JSON.prototype = new Object();
_JSON.prototype.parse = function () { return _; };
_JSON.prototype.stringify = function () { return ""; };

JSON = new _JSON();

function String() {
 this.length = 0;
 this._i_ = "";
}
String.prototype = new Object();
String.prototype.toLowerCase = function () { return ""; };
String.prototype.match = function (pattern) { return _string_; };
String.prototype.replace = function (pattern,pattern1) { return ""; };
String.prototype.concat = function (other) { return ""; };
String.prototype.localeCompare = function (other) { return 0; };
String.prototype.substr = function (from,from1) { return ""; };
String.prototype.substring = function (from,from1) { return ""; };
String.prototype.lastIndexOf = function (char,char1) { return 0; };
String.prototype.toLocaleUpperCase = function () { return ""; };
String.prototype.toLocaleLowerCase = function () { return ""; };
String.prototype.search = function (pattern) { return 0; };
String.prototype.split = function (pattern_,pattern_1) { return _string_; };
String.prototype.trim = function () { return ""; };
String.prototype.slice = function (from,from1) { return ""; };
String.prototype.toUpperCase = function () { return ""; };
String.prototype.charCodeAt = function (i) { return 0; };
String.prototype.indexOf = function (char,char1) { return 0; };
String.prototype.charAt = function (i) { return ""; };
String.fromCharCode = function (code) { return ""; };

function isNaN(value) { return false; }

function _Math() {
 this.LN10 = 0;
 this.E = 0;
 this.LOG10E = 0;
 this.SQRT1_2 = 0;
 this.LOG2E = 0;
 this.SQRT2 = 0;
 this.LN2 = 0;
 this.PI = 0;
}
_Math.prototype = new Object();
_Math.prototype.log = function (number) { return 0; };
_Math.prototype.cos = function (number) { return 0; };
_Math.prototype.atan = function (number) { return 0; };
_Math.prototype.random = function () { return 0; };
_Math.prototype.min = function (number,number1) { return 0; };
_Math.prototype.sqrt = function (number) { return 0; };
_Math.prototype.sin = function (number) { return 0; };
_Math.prototype.pow = function (number,number1) { return 0; };
_Math.prototype.floor = function (number) { return 0; };
_Math.prototype.exp = function (number) { return 0; };
_Math.prototype.atan2 = function (y,y1) { return 0; };
_Math.prototype.tan = function (number) { return 0; };
_Math.prototype.max = function (number,number1) { return 0; };
_Math.prototype.acos = function (number) { return 0; };
_Math.prototype.ceil = function (number) { return 0; };
_Math.prototype.abs = function (number) { return 0; };
_Math.prototype.round = function (number) { return 0; };
_Math.prototype.asin = function (number) { return 0; };

Math = new _Math();

undefined = new Object();

function isFinite(value) { return false; }

function Function() {
 this.prototype = new Object();
}
Function.prototype = new Object();
Function.prototype.call = function (_this,_this1) { return _this__ret; };
Function.prototype.bind = function (_this,_this1) { return _custom_Function_bind; };
Function.prototype.apply = function (_this,_this1) { };

function encodeURI(uri) { return ""; }

function Number() {
}
Number.prototype = new Object();
Number.prototype.toFixed = function (digits) { return ""; };
Number.prototype.toPrecision = function (digits) { return ""; };
Number.prototype.toString = function (radix_) { return ""; };
Number.prototype.toExponential = function (digits) { return ""; };
Number.POSITIVE_INFINITY = 0;
Number.NEGATIVE_INFINITY = 0;
Number.MAX_VALUE = 0;
Number.MIN_VALUE = 0;

function SyntaxError() {
}
SyntaxError.prototype = new Error();

function RangeError() {
}
RangeError.prototype = new Error();

NaN = 0;

function Boolean() {
}
Boolean.prototype = new Object();

Infinity = 0;

function RegExp() {
 this.ignoreCase = false;
 this.multiline = false;
 this.global = false;
 this.source = "";
 this.lastIndex = 0;
}
RegExp.prototype = new Object();
RegExp.prototype.test = function (input) { return false; };
RegExp.prototype.exec = function (input) { return _string_; };

function Error() {
 this.name = "";
 this.message = "";
}
Error.prototype = new Object();

function EvalError() {
}
EvalError.prototype = new Error();

function decodeURI(uri) { return ""; }

function Date() {
}
Date.prototype = new Object();
Date.prototype.setUTCSeconds = function (sec) { return 0; };
Date.prototype.setFullYear = function (year) { return 0; };
Date.prototype.getTime = function () { return 0; };
Date.prototype.toUTCString = function () { return ""; };
Date.prototype.toDateString = function () { return ""; };
Date.prototype.setUTCHours = function (hour) { return 0; };
Date.prototype.getMilliseconds = function () { return 0; };
Date.prototype.getUTCHours = function () { return 0; };
Date.prototype.getFullYear = function () { return 0; };
Date.prototype.getUTCDate = function () { return 0; };
Date.prototype.getHours = function () { return 0; };
Date.prototype.setHours = function (hour) { return 0; };
Date.prototype.setSeconds = function (sec) { return 0; };
Date.prototype.setUTCMinutes = function (min) { return 0; };
Date.prototype.setUTCFullYear = function (year) { return 0; };
Date.prototype.getDate = function () { return 0; };
Date.prototype.toTimeString = function () { return ""; };
Date.prototype.getMonth = function () { return 0; };
Date.prototype.setMilliseconds = function (ms) { return 0; };
Date.prototype.setTime = function (date) { return 0; };
Date.prototype.setMonth = function (month) { return 0; };
Date.prototype.setUTCMilliseconds = function (ms) { return 0; };
Date.prototype.toLocaleDateString = function () { return ""; };
Date.prototype.getUTCMonth = function () { return 0; };
Date.prototype.setMinutes = function (min) { return 0; };
Date.prototype.getUTCSeconds = function () { return 0; };
Date.prototype.getDay = function () { return 0; };
Date.prototype.getMinutes = function () { return 0; };
Date.prototype.setUTCDate = function (day) { return 0; };
Date.prototype.toLocaleTimeString = function () { return ""; };
Date.prototype.getUTCDay = function () { return 0; };
Date.prototype.getSeconds = function () { return 0; };
Date.prototype.getTimezoneOffset = function () { return 0; };
Date.prototype.setUTCMonth = function (month) { return 0; };
Date.prototype.getYear = function () { return 0; };
Date.prototype.getUTCMinutes = function () { return 0; };
Date.prototype.getUTCMilliseconds = function () { return 0; };
Date.prototype.setDate = function (day) { return 0; };
Date.prototype.toISOString = function () { return ""; };
Date.UTC = function (year,year1,year2,year3,year4,year5,year6) { return 0; };
Date.now = function () { return 0; };
Date.parse = function (source) { return _Date; };

function decodeURIComponent(uri) { return ""; }

function Array() {
 this.length = 0;
}
Array.prototype = new Object();
Array.prototype.reduce = function () { return _0__ret; };
Array.prototype.some = function () { return false; };
Array.prototype.splice = function (pos,pos1,pos2) { return ___; };
Array.prototype.forEach = function () { };
Array.prototype.shift = function () { return _this__i_; };
Array.prototype.concat = function (other) { return _this; };
Array.prototype.sort = function () { return number_; };
Array.prototype.reverse = function () { };
Array.prototype.push = function (newelt) { return 0; };
Array.prototype.pop = function () { return _this__i_; };
Array.prototype.filter = function () { return _this; };
Array.prototype.lastIndexOf = function (elt,elt1) { return 0; };
Array.prototype.slice = function (from_,from_1) { return _this; };
Array.prototype.unshift = function (newelt) { return 0; };
Array.prototype.join = function (separator_) { return ""; };
Array.prototype.reduceRight = function () { return _0__ret; };
Array.prototype.indexOf = function (elt,elt1) { return 0; };
Array.prototype.every = function () { return false; };
Array.prototype.map = function () { return __0__ret_; };
Array.isArray = function (value) { return false; };

function parseInt(string,string1) { return 0; }

function eval(code) { return _; }

function TypeError() {
}
TypeError.prototype = new Error();

function Object() {
}
Object.prototype = new Object();
Object.prototype.isPrototypeOf = function (obj) { return false; };
Object.prototype.hasOwnProperty = function (prop) { return false; };
Object.prototype.propertyIsEnumerable = function (prop) { return false; };
Object.prototype.valueOf = function () { return 0; };
Object.prototype.toString = function () { return ""; };
Object.prototype.toLocaleString = function () { return ""; };
Object.isSealed = function (obj) { return false; };
Object.getOwnPropertyNames = function (obj) { return _string_; };
Object.keys = function (obj) { return _string_; };
Object.seal = function (obj) { };
Object.defineProperty = function (obj,obj1,obj2) { return _custom_Object_defineProperty; };
Object.getPrototypeOf = function (obj) { return _; };
Object.freeze = function (obj) { return _0; };
Object.isExtensible = function (obj) { return false; };
Object.defineProperties = function (obj,obj1) { return _custom_Object_defineProperties; };
Object.create = function (proto) { return _custom_Object_create; };
Object.getOwnPropertyDescriptor = function (obj,obj1) { return _; };
Object.isFrozen = function (obj) { return false; };
Object.preventExtensions = function (obj) { };

function encodeURIComponent(uri) { return ""; }

function _Window() {
 this.JSON = new _JSON();
 this.Math = new _Math();
 this.undefined = new Object();
 this.NaN = 0;
 this.Infinity = 0;
}
_Window.prototype = new Object();
_Window.prototype.parseFloat = function parseFloat(string) { return 0; };
_Window.prototype.isNaN = function isNaN(value) { return false; };
_Window.prototype.isFinite = function isFinite(value) { return false; };
_Window.prototype.encodeURI = function encodeURI(uri) { return ""; };
_Window.prototype.decodeURI = function decodeURI(uri) { return ""; };
_Window.prototype.decodeURIComponent = function decodeURIComponent(uri) { return ""; };
_Window.prototype.parseInt = function parseInt(string,string1) { return 0; };
_Window.prototype.eval = function eval(code) { return _; };
_Window.prototype.encodeURIComponent = function encodeURIComponent(uri) { return ""; };

