parent = new Object();

function HTMLQuoteElement() {
}
HTMLQuoteElement.prototype = new Element();

onfocus = new Object();

function scrollBy(x,x1) { }

ondragend = new Object();

function Node() {
 this.NOTATION_NODE = 0;
 this.ENTITY_REFERENCE_NODE = 0;
 this.prefix = "";
 this.nextSibling = new Node();
 this.parentElement = new Element();
 this.DOCUMENT_POSITION_PRECEDING = 0;
 this.DOCUMENT_POSITION_CONTAINED_BY = 0;
 this.firstChild = new Node();
 this.previousSibling = new Node();
 this.CDATA_SECTION_NODE = 0;
 this.childNodes = new NodeList();
 this.ENTITY_NODE = 0;
 this.DOCUMENT_NODE = 0;
 this.textContent = "";
 this.parentNode = new Node();
 this.nodeType = 0;
 this.ELEMENT_NODE = 0;
 this.localName = "";
 this.DOCUMENT_TYPE_NODE = 0;
 this.DOCUMENT_FRAGMENT_NODE = 0;
 this.nodeValue = "";
 this.ATTRIBUTE_NODE = 0;
 this.ownerDocument = new Document();
 this.nodeName = "";
 this.COMMENT_NODE = 0;
 this.DOCUMENT_POSITION_CONTAINS = 0;
 this.TEXT_NODE = 0;
 this.lastChild = new Node();
 this.DOCUMENT_POSITION_FOLLOWING = 0;
 this.namespaceURI = "";
 this.PROCESSING_INSTRUCTION_NODE = 0;
 this.tagName = "";
 this.DOCUMENT_POSITION_DISCONNECTED = 0;
 this.baseURI = "";
 this.attributes = new NamedNodeMap();
 this.DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC = 0;
}
Node.prototype = new Object();
Node.prototype.lookupPrefix = function (uri) { return ""; };
Node.prototype.removeChild = function (oldNode) { return _Node; };
Node.prototype.insertBefore = function (newElt,newElt1) { return _Element; };
Node.prototype.normalize = function () { };
Node.prototype.isSameNode = function (other) { return false; };
Node.prototype.appendChild = function (newNode) { return _Node; };
Node.prototype.replaceChild = function (newChild,newChild1) { return _Node; };
Node.prototype.dispatchEvent = function (event) { return false; };
Node.prototype.isSupported = function (features,features1) { return false; };
Node.prototype.contains = function (other) { return false; };
Node.prototype.isEqualNode = function (other) { return false; };
Node.prototype.hasChildNodes = function () { return false; };
Node.prototype.lookupNamespaceURI = function (uri) { return ""; };
Node.prototype.cloneNode = function (deep) { return _Element; };
Node.prototype.hasAttributes = function () { return false; };
Node.prototype.isDefaultNamespace = function (uri) { return false; };
Node.prototype.compareDocumentPosition = function (other) { return 0; };
Node.prototype.removeEventListener = function () { };
Node.prototype.addEventListener = function () { };

function HTMLInputElement() {
}
HTMLInputElement.prototype = new Element();

function KeyboardEvent() {
}
KeyboardEvent.prototype = new Event();

function focus() { }

function Document() {
 this.activeElement = new Element();
 this.characterSet = "";
 this.plugins = new HTMLCollection();
 this.dir = "";
 this.body = new Element();
 this.readyState = "";
 this.links = new HTMLCollection();
 this.height = 0;
 this.cookie = "";
 this.inputEncoding = "";
 this.domain = "";
 this.designMode = "";
 this.lastModified = "";
 this.xmlEncoding = "";
 this.embeds = new HTMLCollection();
 this.styleSheets = new HTMLCollection();
 this.documentElement = new Element();
 this.querySelectorAll = Element.prototype.querySelectorAll;
 this.anchors = new HTMLCollection();
 this.title = "";
 this.URL = "";
 this.head = new Element();
 this.scripts = new HTMLCollection();
 this.documentURI = "";
 this.currentScript = new Node();
 this.implementation = new _Implementation();
 this.querySelector = Element.prototype.querySelector;
 this.doctype = new Node();
 this.referrer = "";
 this.defaultView = new _DefaultView();
 this.xmlStandalone = false;
 this.width = 0;
 this.location = location;
 this.getElementsByClassName = Element.prototype.getElementsByClassName;
 this.xmlVersion = "";
 this.forms = new HTMLCollection();
 this.compatMode = "";
}
Document.prototype = new Node();
Document.prototype.queryCommandValue = function (cmd) { return ""; };
Document.prototype.queryCommandSupported = function (cmd) { return false; };
Document.prototype.adoptNode = function (node) { return _Element; };
Document.prototype.write = function (html) { };
Document.prototype.elementFromPoint = function (x,x1) { return _Element; };
Document.prototype.queryCommandIndeterm = function (cmd) { return false; };
Document.prototype.createCDATASection = function (content) { return _Node; };
Document.prototype.writeln = function (html) { };
Document.prototype.createElement = function (tagName) { return _Element; };
Document.prototype.createTreeWalker = function (root,root1) { return _; };
Document.prototype.queryCommandEnabled = function (cmd) { return false; };
Document.prototype.createProcessingInstruction = function (content) { return _Node; };
Document.prototype.evaluate = function (expr) { return _XPathResult; };
Document.prototype.getElementById = function (id) { return _Element; };
Document.prototype.createTextNode = function (content) { return _Text; };
Document.prototype.queryCommandState = function (cmd) { return false; };
Document.prototype.createComment = function (content) { return _Node; };
Document.prototype.getElementsByTagNameNS = function (ns,ns1) { return _NodeList; };
Document.prototype.getElementsByName = function (name) { return _HTMLCollection; };
Document.prototype.createEvent = function (type) { return _Event; };
Document.prototype.hasFocus = function () { return false; };
Document.prototype.close = function () { };
Document.prototype.getElementsByTagName = function (tagName) { return _NodeList; };
Document.prototype.execCommand = function (cmd) { };
Document.prototype.createAttributeNS = function (ns,ns1) { return _Attr; };
Document.prototype.createElementNS = function (ns,ns1) { return _Element; };
Document.prototype.clear = function () { };
Document.prototype.importNode = function (node,node1) { return _Element; };
Document.prototype.createAttribute = function (name) { return _Attr; };
Document.prototype.getSelection = function () { return _Selection; };
Document.prototype.createRange = function () { return _Range; };
Document.prototype.createDocumentFragment = function () { return _DocumentFragment; };
Document.prototype.registerElement = function (type,type1) { };
Document.prototype.createNSResolver = function (node) { };
Document.prototype.open = function () { };
Document.prototype.createExpression = function (text) { return _; };

function _Implementation() {
}
_Implementation.prototype = new Object();
_Implementation.prototype.createHTMLDocument = function (title) { return _Document; };
_Implementation.prototype.hasFeature = function (feature,feature1) { return false; };
_Implementation.prototype.createDocumentType = function (qualifiedName,qualifiedName1,qualifiedName2) { return _Node; };
_Implementation.prototype.createDocument = function (namespaceURI,namespaceURI1,namespaceURI2) { return _Document; };

function _DefaultView() {
}
_DefaultView.prototype = new Object();

function Image(width_,width_1) { return _HTMLImageElement; }

function XMLDocument() {
}
XMLDocument.prototype = new Document();

function Attr() {
 this.isId = false;
 this.name = "";
 this.value = "";
}
Attr.prototype = new Object();

onmessage = new Object();

function HTMLSpanElement() {
}
HTMLSpanElement.prototype = new Element();

function HTMLTableDataCellElement() {
}
HTMLTableDataCellElement.prototype = new Element();

function HTMLCollection() {
 this._i_ = new Element();
 this.length = 0;
}
HTMLCollection.prototype = new Object();
HTMLCollection.prototype.item = function (i) { return _Element; };
HTMLCollection.prototype.namedItem = function (name) { return _Element; };

function HTMLTableColElement() {
}
HTMLTableColElement.prototype = new Element();

function clearTimeout(timeout) { }

function HTMLTableElement() {
}
HTMLTableElement.prototype = new Element();

onscroll = new Object();

function HTMLDivElement() {
}
HTMLDivElement.prototype = new Element();

function DOMParser() {
}
DOMParser.prototype = new Object();
DOMParser.prototype.parseFromString = function (data,data1) { return _Document; };

onkeydown = new Object();

function HTMLTableCaptionElement() {
}
HTMLTableCaptionElement.prototype = new Element();

function HTMLTitleElement() {
}
HTMLTitleElement.prototype = new Element();

function HTMLUnknownElement() {
}
HTMLUnknownElement.prototype = new Element();

function HTMLModElement() {
}
HTMLModElement.prototype = new Element();

function dispatchEvent(event) { return false; }

function HTMLElement() {
}
HTMLElement.prototype = new Element();

function HTMLTimeElement() {
}
HTMLTimeElement.prototype = new Element();

function NodeList() {
 this._i_ = new Element();
 this.length = 0;
}
NodeList.prototype = new Object();
NodeList.prototype.item = function (i) { return _Element; };

onclick = new Object();

onkeyup = new Object();

function scroll(x,x1) { }

function HTMLLIElement() {
}
HTMLLIElement.prototype = new Element();

onchange = new Object();

onkeypress = new Object();

function scrollTo(x,x1) { }

ondblclick = new Object();

function HTMLKeygenElement() {
}
HTMLKeygenElement.prototype = new Element();

function HTMLStyleElement() {
}
HTMLStyleElement.prototype = new Element();

ondrag = new Object();

pageYOffset = 0;

outerHeight = 0;

function HTMLOptionElement() {
}
HTMLOptionElement.prototype = new Element();

onbeforeunload = new Object();

function HTMLTrackElement() {
}
HTMLTrackElement.prototype = new Element();

onoffline = new Object();

screenTop = 0;

function HTMLEmbedElement() {
}
HTMLEmbedElement.prototype = new Element();

onmousemove = new Object();

oncontextmenu = new Object();

HTMLDocument = Document;

function HTMLOptionsCollection() {
}
HTMLOptionsCollection.prototype = new Element();

function Selection() {
 this.isCollapsed = false;
 this.anchorOffset = 0;
 this.focusNode = new Element();
 this.focusOffset = 0;
 this.rangeCount = 0;
 this.anchorNode = new Element();
}
Selection.prototype = new Object();
Selection.prototype.getRangeAt = function (i) { return _Range; };
Selection.prototype.collapseToStart = function () { };
Selection.prototype.selectAllChildren = function (node) { };
Selection.prototype.collapseToEnd = function () { };
Selection.prototype.addRange = function (range) { };
Selection.prototype.extend = function (node,node1) { };
Selection.prototype.removeAllRanges = function () { };
Selection.prototype.removeRange = function (range) { };
Selection.prototype.deleteFromDocument = function () { };
Selection.prototype.containsNode = function (node) { return false; };
Selection.prototype.collapse = function () { };

frameElement = new Element();

document = new Document();

function blur() { }

onmouseover = new Object();

function _URL() {
}
_URL.prototype = new Object();
_URL.prototype.revokeObjectURL = function (string) { };
_URL.prototype.createObjectURL = function (blob) { return ""; };

URL = new _URL();

function _CanvasRenderingContext2D() {
 this.shadowBlur = 0;
 this.fillRule = "";
 this.shadowColor = "";
 this.height = 0;
 this.canvas = new Element();
 this.strokeStyle = "";
 this.textAlign = "";
 this.miterLimit = 0;
 this.lineCap = "";
 this.fillStyle = "";
 this.lineDashOffset = 0;
 this.shadowOffsetX = 0;
 this.shadowOffsetY = 0;
 this.imageSmoothingEnabled = false;
 this.lineWidth = 0;
 this.globalAlpha = 0;
 this.direction = "";
 this.textBaseline = "";
 this.globalCompositeOperation = "";
 this.lineJoin = "";
 this.currentTransform = new Object();
 this.width = 0;
 this.font = "";
}
_CanvasRenderingContext2D.prototype = new Object();
_CanvasRenderingContext2D.prototype.rotate = function (angle) { };
_CanvasRenderingContext2D.prototype.closePath = function () { };
_CanvasRenderingContext2D.prototype.commit = function () { };
_CanvasRenderingContext2D.prototype.fillRect = function (x,x1,x2,x3) { };
_CanvasRenderingContext2D.prototype.translate = function (x,x1) { };
_CanvasRenderingContext2D.prototype.ellipse = function (x,x1,x2,x3,x4,x5,x6,x7) { };
_CanvasRenderingContext2D.prototype.resetClip = function () { };
_CanvasRenderingContext2D.prototype.resetTransform = function () { };
_CanvasRenderingContext2D.prototype.fill = function () { };
_CanvasRenderingContext2D.prototype.getLineDash = function () { return _number_; };
_CanvasRenderingContext2D.prototype.createPattern = function (image,image1) { return _; };
_CanvasRenderingContext2D.prototype.createLinearGradient = function (x0,x01,x02,x03) { return _; };
_CanvasRenderingContext2D.prototype.createImageData = function (sw,sw1) { return _; };
_CanvasRenderingContext2D.prototype.save = function () { };
_CanvasRenderingContext2D.prototype.scale = function (x,x1) { };
_CanvasRenderingContext2D.prototype.lineTo = function (x,x1) { };
_CanvasRenderingContext2D.prototype.measureText = function (text) { return _; };
_CanvasRenderingContext2D.prototype.setLineDash = function (segments) { };
_CanvasRenderingContext2D.prototype.putImageData = function (imagedata,imagedata1,imagedata2) { };
_CanvasRenderingContext2D.prototype.rect = function (x,x1,x2,x3) { };
_CanvasRenderingContext2D.prototype.transform = function (a,a1,a2,a3,a4,a5) { };
_CanvasRenderingContext2D.prototype.arc = function (x,x1,x2,x3,x4,x5) { };
_CanvasRenderingContext2D.prototype.setTransform = function (a,a1,a2,a3,a4,a5) { };
_CanvasRenderingContext2D.prototype.strokeRect = function (x,x1,x2,x3) { };
_CanvasRenderingContext2D.prototype.fillText = function (text,text1,text2,text3) { };
_CanvasRenderingContext2D.prototype.getImageData = function (sx,sx1,sx2,sx3) { return _; };
_CanvasRenderingContext2D.prototype.arcTo = function (x1,x11,x12,x13,x14) { };
_CanvasRenderingContext2D.prototype.strokeText = function (text,text1,text2,text3) { };
_CanvasRenderingContext2D.prototype.restore = function () { };
_CanvasRenderingContext2D.prototype.beginPath = function () { };
_CanvasRenderingContext2D.prototype.quadraticCurveTo = function (cpx,cpx1,cpx2,cpx3) { };
_CanvasRenderingContext2D.prototype.bezierCurveTo = function (cp1x,cp1x1,cp1x2,cp1x3,cp1x4,cp1x5) { };
_CanvasRenderingContext2D.prototype.stroke = function () { };
_CanvasRenderingContext2D.prototype.clearRect = function (x,x1,x2,x3) { };
_CanvasRenderingContext2D.prototype.drawImage = function (image,image1,image2) { };
_CanvasRenderingContext2D.prototype.clip = function () { };
_CanvasRenderingContext2D.prototype.moveTo = function (x,x1) { };

CanvasRenderingContext2D = new _CanvasRenderingContext2D();

function HTMLAudioElement() {
}
HTMLAudioElement.prototype = new Element();

top = new Object();

function _SessionStorage() {
}
_SessionStorage.prototype = new Object();
_SessionStorage.prototype.getItem = function (name) { return ""; };
_SessionStorage.prototype.setItem = function (name,name1) { };

sessionStorage = new _SessionStorage();

function HTMLMetaElement() {
}
HTMLMetaElement.prototype = new Element();

function Worker() {
 this.onmessage = new Object();
 this.onerror = new Object();
}
Worker.prototype = new Object();
Worker.prototype.postMessage = function (message) { };
Worker.prototype.terminate = function () { };

ondragenter = new Object();

function HTMLBaseElement() {
}
HTMLBaseElement.prototype = new Element();

function HTMLDataListElement() {
}
HTMLDataListElement.prototype = new Element();

function HTMLMapElement() {
}
HTMLMapElement.prototype = new Element();

function close() { }

function HTMLHtmlElement() {
}
HTMLHtmlElement.prototype = new Element();

onmousewheel = new Object();

onblur = new Object();

function MouseEvent() {
}
MouseEvent.prototype = new Event();

function Text() {
 this.wholeText = "";
}
Text.prototype = new Node();
Text.prototype.splitText = function (offset) { return _Text; };

function atob(encoded) { return ""; }

function HTMLSelectElement() {
}
HTMLSelectElement.prototype = new Element();

function confirm(message) { return false; }

function HTMLHRElement() {
}
HTMLHRElement.prototype = new Element();

function HTMLDListElement() {
}
HTMLDListElement.prototype = new Element();

function setTimeout() { return 0; }

function HTMLScriptElement() {
}
HTMLScriptElement.prototype = new Element();

onmouseout = new Object();

function ErrorEvent() {
}
ErrorEvent.prototype = new Event();

function Event() {
 this.MOUSEMOVE = 0;
 this.dataTransfer = new _DataTransfer();
 this.MOUSEUP = 0;
 this.returnValue = false;
 this.FOCUS = 0;
 this.ctrlKey = false;
 this.type = "";
 this.MOUSEOUT = 0;
 this.MOUSEOVER = 0;
 this.button = 0;
 this.shiftKey = false;
 this.AT_TARGET = 0;
 this.KEYDOWN = 0;
 this.CHANGE = 0;
 this.KEYUP = 0;
 this.altKey = false;
 this.NONE = 0;
 this.which = 0;
 this.DRAGDROP = 0;
 this.clientY = 0;
 this.clientX = 0;
 this.charCode = 0;
 this.metaKey = false;
 this.MOUSEDRAG = 0;
 this.SELECT = 0;
 this.target = new Element();
 this.cancelBubble = false;
 this.CAPTURING_PHASE = 0;
 this.keyCode = 0;
 this.MOUSEDOWN = 0;
 this.DBLCLICK = 0;
 this.relatedTarget = new Element();
 this.KEYPRESS = 0;
 this.BLUR = 0;
 this.BUBBLING_PHASE = 0;
 this.CLICK = 0;
 this.pageY = 0;
 this.pageX = 0;
}
Event.prototype = new Object();
Event.prototype.initEvent = function (type,type1,type2) { };
Event.prototype.preventDefault = function () { };
Event.prototype.stopPropagation = function () { };
Event.prototype.stopImmediatePropagation = function () { };

function _DataTransfer() {
 this.dropEffect = "";
 this.types = new Array();
 this.files = new FileList();
 this.effectAllowed = "";
}
_DataTransfer.prototype = new Object();
_DataTransfer.prototype.setData = function (type,type1) { };
_DataTransfer.prototype.addElement = function (element) { };
_DataTransfer.prototype.setDragImage = function (image) { };
_DataTransfer.prototype.getData = function (type) { return ""; };
_DataTransfer.prototype.clearData = function (type_) { };

self = new Object();

closed = false;

function XMLHttpRequest() {
 this.LOADING = 0;
 this.DONE = 0;
 this.responseText = "";
 this.responseXML = new Document();
 this.HEADERS_RECEIVED = 0;
 this.UNSENT = 0;
 this.timeout = 0;
 this.responseType = "";
 this.response = new Document();
 this.statusText = "";
 this.readyState = 0;
 this.status = 0;
 this.OPENED = 0;
}
XMLHttpRequest.prototype = new Object();
XMLHttpRequest.prototype.onreadystatechange = function () { };
XMLHttpRequest.prototype.getAllResponseHeaders = function () { return ""; };
XMLHttpRequest.prototype.setRequestHeader = function (header,header1) { };
XMLHttpRequest.prototype.overrideMimeType = function (type) { };
XMLHttpRequest.prototype.abort = function () { };
XMLHttpRequest.prototype.getResponseHeader = function (header) { return ""; };
XMLHttpRequest.prototype.send = function (data_) { };
XMLHttpRequest.prototype.open = function (method,method1,method2,method3,method4) { };

onmousedown = new Object();

function HTMLProgressElement() {
}
HTMLProgressElement.prototype = new Element();

function CustomEvent() {
}
CustomEvent.prototype = new Event();

function HTMLFormControlsCollection() {
}
HTMLFormControlsCollection.prototype = new Element();

function DOMTokenList() {
 this._i_ = "";
 this.length = 0;
}
DOMTokenList.prototype = new Object();
DOMTokenList.prototype.add = function (token) { };
DOMTokenList.prototype.item = function (i) { return ""; };
DOMTokenList.prototype.contains = function (token) { return false; };
DOMTokenList.prototype.toggle = function (token) { return false; };
DOMTokenList.prototype.remove = function (token) { };

function HTMLOListElement() {
}
HTMLOListElement.prototype = new Element();

function HTMLOptGroupElement() {
}
HTMLOptGroupElement.prototype = new Element();

function HTMLTableRowElement() {
}
HTMLTableRowElement.prototype = new Element();

function BeforeLoadEvent() {
}
BeforeLoadEvent.prototype = new Event();

function TouchEvent() {
}
TouchEvent.prototype = new Event();

screenLeft = 0;

function _Navigator() {
 this.appVersion = "";
 this.javaEnabled = false;
 this.appName = "";
 this.plugins = new Array();
 this.vendor = "";
 this.language = "";
 this.userAgent = "";
 this.platform = "";
}
_Navigator.prototype = new Object();

navigator = new _Navigator();

function _Screen() {
 this.availWidth = 0;
 this.pixelDepth = 0;
 this.availTop = 0;
 this.width = 0;
 this.colorDepth = 0;
 this.availLeft = 0;
 this.availHeight = 0;
 this.height = 0;
}
_Screen.prototype = new Object();

screen = new _Screen();

function btoa(data) { return ""; }

outerWidth = 0;

function HTMLMeterElement() {
}
HTMLMeterElement.prototype = new Element();

function HTMLLabelElement() {
}
HTMLLabelElement.prototype = new Element();

onresize = new Object();

function HTMLParamElement() {
}
HTMLParamElement.prototype = new Element();

function HTMLImageElement() {
}
HTMLImageElement.prototype = new Element();

function HTMLFormElement() {
}
HTMLFormElement.prototype = new Element();

function HTMLOutputElement() {
}
HTMLOutputElement.prototype = new Element();

function HTMLUListElement() {
}
HTMLUListElement.prototype = new Element();

function postMessage(message,message1) { }

function HashChangeEvent() {
}
HashChangeEvent.prototype = new Event();

function HTMLTableCellElement() {
}
HTMLTableCellElement.prototype = new Element();

function HTMLButtonElement() {
}
HTMLButtonElement.prototype = new Element();

function HTMLHeadingElement() {
}
HTMLHeadingElement.prototype = new Element();

pageXOffset = 0;

function FormData() {
}
FormData.prototype = new Object();
FormData.prototype.getAll = function (name) { };
FormData.prototype.set = function (name,name1,name2) { };
FormData.prototype.get = function (name) { };
FormData.prototype.has = function (name) { };
FormData.prototype._delete = function (name) { };
FormData.prototype.append = function (name,name1,name2) { };

function WheelEvent() {
}
WheelEvent.prototype = new Event();

function Blob() {
 this.size = 0;
 this.type = "";
}
Blob.prototype = new Object();
Blob.prototype.slice = function (start,start1,start2) { return _Blob; };

innerHeight = 0;

function FileList() {
 this._i_ = new File();
 this.length = 0;
}
FileList.prototype = new Object();
FileList.prototype.item = function (i) { return _File; };

function HTMLLegendElement() {
}
HTMLLegendElement.prototype = new Element();

function _History() {
 this.length = 0;
 this.state = new Object();
}
_History.prototype = new Object();
_History.prototype.pushState = function (data,data1,data2) { };
_History.prototype.forward = function () { };
_History.prototype.go = function (delta) { };
_History.prototype.back = function () { };
_History.prototype.replaceState = function (data,data1,data2) { };

history = new _History();

function Range() {
 this.endOffset = 0;
 this.collapsed = false;
 this.endContainer = new Element();
 this.commonAncestorContainer = new Element();
 this.startContainer = new Element();
 this.START_TO_START = 0;
 this.startOffset = 0;
 this.START_TO_END = 0;
 this.END_TO_START = 0;
 this.END_TO_END = 0;
}
Range.prototype = new Object();
Range.prototype.cloneRange = function () { return _Range; };
Range.prototype.insertNode = function (node) { };
Range.prototype.cloneContents = function () { return _DocumentFragment; };
Range.prototype.setStart = function (node,node1) { };
Range.prototype.selectNode = function (node) { };
Range.prototype.selectNodeContents = function (node) { };
Range.prototype.setStartBefore = function (node) { };
Range.prototype.setEnd = function (node,node1) { };
Range.prototype.extractContents = function () { return _DocumentFragment; };
Range.prototype.setEndBefore = function (node) { };
Range.prototype.setStartAfter = function (node) { };
Range.prototype.surroundContents = function (node) { };
Range.prototype.deleteContents = function () { };
Range.prototype.detach = function () { };
Range.prototype.setEndAfter = function (node) { };
Range.prototype.compareBoundaryPoints = function (how,how1) { return 0; };
Range.prototype.collapse = function (toStart) { };

function _Crypto() {
}
_Crypto.prototype = new Object();
_Crypto.prototype.getRandomValues = function (_number_) { };

crypto = new _Crypto();

function HTMLLinkElement() {
}
HTMLLinkElement.prototype = new Element();

innerWidth = 0;

opener = new Object();

name = "";

function HTMLMediaElement() {
}
HTMLMediaElement.prototype = new Element();

function HTMLFieldSetElement() {
}
HTMLFieldSetElement.prototype = new Element();

function HTMLHeadElement() {
}
HTMLHeadElement.prototype = new Element();

onabort = new Object();

function File() {
 this.fileName = "";
 this.fileSize = 0;
 this.lastModifiedDate = new Object();
 this.name = "";
}
File.prototype = new Blob();

screenX = 0;

screenY = 0;

onmouseup = new Object();

onerror = new Object();

function HTMLVideoElement() {
}
HTMLVideoElement.prototype = new Element();

function HTMLIFrameElement() {
}
HTMLIFrameElement.prototype = new Element();

ondragover = new Object();

function Element() {
 this.onfocus = new Object();
 this.innerHTML = "";
 this.ondragend = new Object();
 this.contentEditable = false;
 this.lastElementChild = new Element();
 this.scrollHeight = 0;
 this.tabIndex = 0;
 this.clientTop = 0;
 this.onresize = new Object();
 this.clientWidth = 0;
 this.children = new HTMLCollection();
 this.firstElementChild = new Element();
 this.onscroll = new Object();
 this.offsetLeft = 0;
 this.onkeydown = new Object();
 this.scrollLeft = 0;
 this.height = 0;
 this.onclick = new Object();
 this.onkeyup = new Object();
 this.onchange = new Object();
 this.clientHeight = 0;
 this.nextElementSibling = new Element();
 this.scrollTop = 0;
 this.onkeypress = new Object();
 this.ondblclick = new Object();
 this.ondrag = new Object();
 this.onbeforeunload = new Object();
 this.style = new _Style();
 this.clientLeft = 0;
 this.onmousemove = new Object();
 this.oncontextmenu = new Object();
 this.onmouseup = new Object();
 this.ondragover = new Object();
 this.className = "";
 this.title = "";
 this.oncut = new Object();
 this.onmouseover = new Object();
 this.ondragenter = new Object();
 this.classList = new DOMTokenList();
 this.ondragleave = new Object();
 this.onmousewheel = new Object();
 this.scrollWidth = 0;
 this.onblur = new Object();
 this.offsetTop = 0;
 this.offsetWidth = 0;
 this.childElementCount = 0;
 this.previousElementSibling = new Element();
 this.ondragstart = new Object();
 this.oncopy = new Object();
 this.onpaste = new Object();
 this.onmouseout = new Object();
 this.offsetHeight = 0;
 this.width = 0;
 this.onmousedown = new Object();
}
Element.prototype = new Node();
Element.prototype.getBoundingClientRect = function () { return _ClientRect; };
Element.prototype.setAttributeNodeNS = function (attr) { return _Attr; };
Element.prototype.removeAttributeNS = function (ns,ns1) { };
Element.prototype.focus = function () { };
Element.prototype.scrollByLines = function (lines) { };
Element.prototype.hasAttributeNS = function (ns,ns1) { return false; };
Element.prototype.scrollByPages = function (pages) { };
Element.prototype.hasAttribute = function (name) { return false; };
Element.prototype.setAttributeNode = function (attr) { return _Attr; };
Element.prototype.attachedCallback = function () { };
Element.prototype.getAttributeNodeNS = function (ns,ns1) { return _Attr; };
Element.prototype.insertAdjacentHTML = function (position,position1) { };
Element.prototype.attributeChangedCallback = function () { };
Element.prototype.detachedCallback = function () { };
Element.prototype.querySelectorAll = function (selectors) { return _NodeList; };
Element.prototype.createdCallback = function () { };
Element.prototype.blur = function () { };
Element.prototype.getElementsByTagNameNS = function (ns,ns1) { return _NodeList; };
Element.prototype.setAttribute = function (name,name1) { };
Element.prototype.getAttribute = function (name) { return ""; };
Element.prototype.scrollIntoView = function (top) { };
Element.prototype.removeAttribute = function (name) { };
Element.prototype.setAttributeNS = function (ns,ns1,ns2) { };
Element.prototype.getElementsByTagName = function (tagName) { return _NodeList; };
Element.prototype.querySelector = function (selectors) { return _Element; };
Element.prototype.getClientRects = function () { return __ClientRect_; };
Element.prototype.getAttributeNode = function (name) { return _Attr; };
Element.prototype.getAttributeNS = function (ns,ns1) { return ""; };
Element.prototype.getElementsByClassName = function (name) { return _NodeList; };
Element.prototype.removeAttributeNode = function (attr) { return _Attr; };
Element.prototype.supportsContext = function (id) { return false; };
Element.prototype.getContext = function (id) { return CanvasRenderingContext2D; };

function _Style() {
 this.kerning = "";
 this.wordSpacing = "";
 this.paddingRight = "";
 this.borderRightStyle = "";
 this.textAnchor = "";
 this.borderTopColor = "";
 this.quotes = "";
 this.marginRight = "";
 this.minHeight = "";
 this.textUnderlineColor = "";
 this.cssText = "";
 this.colorRendering = "";
 this.captionSide = "";
 this.markerEnd = "";
 this.borderStyle = "";
 this.backgroundOrigin = "";
 this.stopColor = "";
 this.textShadow = "";
 this.height = "";
 this.maxWidth = "";
 this.zIndex = "";
 this.verticalAlign = "";
 this.backgroundAttachment = "";
 this.margin = "";
 this.tableLayout = "";
 this.dominantBaseline = "";
 this.pageBreakBefore = "";
 this.floodColor = "";
 this.glyphOrientationVertical = "";
 this.backgroundRepeatY = "";
 this.clipPath = "";
 this.textDecoration = "";
 this.textOverlineWidth = "";
 this.fontStyle = "";
 this.pageBreakInside = "";
 this.borderTopLeftRadius = "";
 this.borderCollapse = "";
 this.borderSpacing = "";
 this.textUnderlineMode = "";
 this.overflowX = "";
 this.overflowY = "";
 this.textUnderline = "";
 this.pageBreakAfter = "";
 this.size = "";
 this.left = "";
 this.pointerEvents = "";
 this.backgroundRepeatX = "";
 this.strokeLinecap = "";
 this.fontSize = "";
 this.backgroundRepeat = "";
 this.paddingLeft = "";
 this.borderImageWidth = "";
 this.floodOpacity = "";
 this.borderImageSlice = "";
 this.lightingColor = "";
 this.emptyCells = "";
 this.boxSizing = "";
 this.stopOpacity = "";
 this.baselineShift = "";
 this.textUnderlineWidth = "";
 this.borderTop = "";
 this.outline = "";
 this.textLineThroughColor = "";
 this.tabSize = "";
 this.textLineThrough = "";
 this.top = "";
 this.maxHeight = "";
 this.imageRendering = "";
 this.colorProfile = "";
 this.textLineThroughMode = "";
 this.backgroundSize = "";
 this.borderTopRightRadius = "";
 this.speak = "";
 this.backgroundPosition = "";
 this.orphans = "";
 this.counterReset = "";
 this.borderLeftColor = "";
 this.strokeDasharray = "";
 this.wordBreak = "";
 this.backgroundColor = "";
 this.display = "";
 this.minWidth = "";
 this.right = "";
 this.borderLeftStyle = "";
 this.alignmentBaseline = "";
 this.filter = "";
 this.outlineColor = "";
 this.width = "";
 this.borderImageOutset = "";
 this.colorInterpolation = "";
 this.lineHeight = "";
 this.opacity = "";
 this.clip = "";
 this.marginTop = "";
 this.font = "";
 this.boxShadow = "";
 this.borderColor = "";
 this.textOverline = "";
 this.fontVariant = "";
 this.float = "";
 this.enableBackground = "";
 this.vectorEffect = "";
 this.writingMode = "";
 this.backgroundClip = "";
 this.borderTopStyle = "";
 this.borderLeft = "";
 this.fontFamily = "";
 this.outlineStyle = "";
 this.overflow = "";
 this.paddingBottom = "";
 this.outlineOffset = "";
 this.borderLeftWidth = "";
 this.fillRule = "";
 this.borderBottomStyle = "";
 this.borderRightColor = "";
 this.borderBottomWidth = "";
 this.backgroundPositionX = "";
 this.borderBottom = "";
 this.strokeMiterlimit = "";
 this.backgroundPositionY = "";
 this.fontWeight = "";
 this.strokeOpacity = "";
 this.textTransform = "";
 this.border = "";
 this.visibility = "";
 this.textAlign = "";
 this.bottom = "";
 this.textLineThroughWidth = "";
 this.shapeRendering = "";
 this.strokeDashoffset = "";
 this.textUnderlineStyle = "";
 this.fill = "";
 this.marginLeft = "";
 this.overflowWrap = "";
 this.borderImageSource = "";
 this.borderRadius = "";
 this.background = "";
 this.borderRight = "";
 this.marker = "";
 this.markerStart = "";
 this.listStyleType = "";
 this.marginBottom = "";
 this.page = "";
 this.position = "";
 this.colorInterpolationFilters = "";
 this.cursor = "";
 this.strokeWidth = "";
 this.fontStretch = "";
 this.strokeLinejoin = "";
 this.color = "";
 this.fillOpacity = "";
 this.backgroundImage = "";
 this.wordWrap = "";
 this.listStyleImage = "";
 this.unicodeRange = "";
 this.textIndent = "";
 this.textOverlineColor = "";
 this.content = "";
 this.borderBottomColor = "";
 this.listStyle = "";
 this.borderRightWidth = "";
 this.borderTopWidth = "";
 this.borderWidth = "";
 this.textRendering = "";
 this.borderBottomRightRadius = "";
 this.glyphOrientationHorizontal = "";
 this.paddingTop = "";
 this.textOverflow = "";
 this.textLineThroughStyle = "";
 this.textOverlineMode = "";
 this.clipRule = "";
 this.direction = "";
 this.mask = "";
 this.padding = "";
 this.whiteSpace = "";
 this.src = "";
 this.borderImage = "";
 this.clear = "";
 this.unicodeBidi = "";
 this.letterSpacing = "";
 this.zoom = "";
 this.stroke = "";
 this.counterIncrement = "";
 this.listStylePosition = "";
 this.outlineWidth = "";
 this.borderBottomLeftRadius = "";
 this.markerMid = "";
 this.resize = "";
 this.textOverlineStyle = "";
 this.borderImageRepeat = "";
}
_Style.prototype = new Object();

function _LocalStorage() {
}
_LocalStorage.prototype = new Object();
_LocalStorage.prototype.getItem = function (name) { return ""; };
_LocalStorage.prototype.setItem = function (name,name1) { };

localStorage = new _LocalStorage();

ononline = new Object();

onhashchange = new Object();

function getComputedStyle(node,node1) { return Element_prototype_style; }

function clearInterval(interval) { }

devicePixelRatio = 0;

oninput = new Object();

function HTMLAreaElement() {
}
HTMLAreaElement.prototype = new Element();

function HTMLTableSectionElement() {
}
HTMLTableSectionElement.prototype = new Element();

function alert(message) { }

function setInterval() { return 0; }

function FileReader() {
 this.LOADING = 0;
 this.DONE = 0;
 this.onerror = new Object();
 this.onloadend = new Object();
 this.error = new Object();
 this.onload = new Object();
 this.result = new Object();
 this.onloadstart = new Object();
 this.onprogress = new Object();
 this.readyState = 0;
 this.onabort = new Object();
 this.EMPTY = 0;
}
FileReader.prototype = new Object();
FileReader.prototype.readAsText = function (blob,blob1) { };
FileReader.prototype.readAsBinaryString = function (blob) { };
FileReader.prototype.readAsDataURL = function (blob) { };
FileReader.prototype.abort = function () { };
FileReader.prototype.readAsArrayBuffer = function (blob) { };

function XPathResult() {
 this.NUMBER_TYPE = 0;
 this.UNORDERED_NODE_ITERATOR_TYPE = 0;
 this.invalidIteratorState = false;
 this.BOOL_TYPE = 0;
 this.UNORDERED_NODE_SNAPSHOT_TYPE = 0;
 this.stringValue = "";
 this.singleNodeValue = new Element();
 this.snapshotLength = 0;
 this.ORDERED_NODE_ITERATOR_TYPE = 0;
 this.FIRST_ORDERED_NODE_TYPE = 0;
 this.STRING_TYPE = 0;
 this.ANY_TYPE = 0;
 this.boolValue = false;
 this.numberValue = 0;
 this.ORDERED_NODE_SNAPSHOT_TYPE = 0;
 this.ANY_UNORDERED_NODE_TYPE = 0;
 this.resultType = 0;
}
XPathResult.prototype = new Object();
XPathResult.prototype.snapshotItem = function () { };
XPathResult.prototype.iterateNext = function () { };

function HTMLTextAreaElement() {
}
HTMLTextAreaElement.prototype = new Element();

function HTMLBRElement() {
}
HTMLBRElement.prototype = new Element();

function HTMLTableHeaderCellElement() {
}
HTMLTableHeaderCellElement.prototype = new Element();

function DocumentFragment() {
}
DocumentFragment.prototype = new Node();

ondragleave = new Object();

function _Console() {
}
_Console.prototype = new Object();
_Console.prototype.timeEnd = function (timerName) { };
_Console.prototype.log = function (text) { };
_Console.prototype.count = function (label_) { };
_Console.prototype.dir = function (object) { };
_Console.prototype.error = function (text) { };
_Console.prototype.warn = function (text) { };
_Console.prototype.groupEnd = function () { };
_Console.prototype.trace = function () { };
_Console.prototype.assert = function (assertion,assertion1) { };
_Console.prototype.time = function (timerName) { };
_Console.prototype.groupCollapsed = function () { };
_Console.prototype.group = function () { };
_Console.prototype.info = function (text) { };

console = new _Console();

function HTMLDataElement() {
}
HTMLDataElement.prototype = new Element();

function HTMLObjectElement() {
}
HTMLObjectElement.prototype = new Element();

function NamedNodeMap() {
 this._i_ = new Node();
 this.length = 0;
}
NamedNodeMap.prototype = new Object();
NamedNodeMap.prototype.removeNamedItem = function (name) { return _Node; };
NamedNodeMap.prototype.item = function (i) { return _Node; };
NamedNodeMap.prototype.setNamedItem = function (node) { return _Node; };
NamedNodeMap.prototype.getNamedItemNS = function (ns,ns1) { return _Node; };
NamedNodeMap.prototype.getNamedItem = function (name) { return _Node; };
NamedNodeMap.prototype.setNamedItemNS = function (node) { return _Node; };
NamedNodeMap.prototype.removeNamedItemNS = function (ns,ns1) { return _Node; };

function HTMLSourceElement() {
}
HTMLSourceElement.prototype = new Element();

function ClientRect() {
 this.top = 0;
 this.left = 0;
 this.bottom = 0;
 this.right = 0;
}
ClientRect.prototype = new Object();

onunload = new Object();

ondrop = new Object();

ondragstart = new Object();

function WebSocket() {
 this.onerror = new Object();
 this.CLOSING = 0;
 this.CLOSED = 0;
 this.onopen = new Object();
 this.url = "";
 this.OPEN = 0;
 this.onmessage = new Object();
 this.extensions = "";
 this.protocol = "";
 this.CONNECTING = 0;
 this.onclose = new Object();
 this.binaryType = "";
 this.bufferedAmount = 0;
}
WebSocket.prototype = new Object();
WebSocket.prototype.close = function () { };
WebSocket.prototype.send = function (data) { };

onload = new Object();

function HTMLParagraphElement() {
}
HTMLParagraphElement.prototype = new Element();

function getSelection() { return _Selection; }

function removeEventListener() { }

function HTMLAnchorElement() {
}
HTMLAnchorElement.prototype = new Element();

function HTMLPreElement() {
}
HTMLPreElement.prototype = new Element();

function _Location() {
 this.origin = "";
 this.pathname = "";
 this.search = "";
 this.hostname = "";
 this.protocol = "";
 this.port = "";
 this.host = "";
 this.href = "";
 this.hash = "";
}
_Location.prototype = new Object();
_Location.prototype.replace = function (url) { };
_Location.prototype.reload = function () { };
_Location.prototype.assign = function (url) { };

location = new _Location();

function HTMLBodyElement() {
}
HTMLBodyElement.prototype = new Element();

scrollY = 0;

function HTMLCanvasElement() {
}
HTMLCanvasElement.prototype = new Element();

scrollX = 0;

function prompt(message,message1) { return ""; }

onpopstate = new Object();

function addEventListener() { }

function _Window() {
 this.parent = new Object();
 this.onfocus = new Object();
 this.ondragend = new Object();
 this.onmessage = new Object();
 this.onscroll = new Object();
 this.onkeydown = new Object();
 this.onclick = new Object();
 this.onkeyup = new Object();
 this.onchange = new Object();
 this.onkeypress = new Object();
 this.ondblclick = new Object();
 this.ondrag = new Object();
 this.pageYOffset = 0;
 this.outerHeight = 0;
 this.onbeforeunload = new Object();
 this.onoffline = new Object();
 this.screenTop = 0;
 this.onmousemove = new Object();
 this.oncontextmenu = new Object();
 this.HTMLDocument = Document;
 this.frameElement = new Element();
 this.document = new Document();
 this.onmouseover = new Object();
 this.URL = new _URL();
 this.CanvasRenderingContext2D = new _CanvasRenderingContext2D();
 this.top = new Object();
 this.sessionStorage = new _SessionStorage();
 this.ondragenter = new Object();
 this.onmousewheel = new Object();
 this.onblur = new Object();
 this.onmouseout = new Object();
 this.self = new Object();
 this.closed = false;
 this.onmousedown = new Object();
 this.screenLeft = 0;
 this.navigator = new _Navigator();
 this.screen = new _Screen();
 this.outerWidth = 0;
 this.onresize = new Object();
 this.pageXOffset = 0;
 this.innerHeight = 0;
 this.history = new _History();
 this.crypto = new _Crypto();
 this.innerWidth = 0;
 this.opener = new Object();
 this.name = "";
 this.onabort = new Object();
 this.screenX = 0;
 this.screenY = 0;
 this.onmouseup = new Object();
 this.onerror = new Object();
 this.ondragover = new Object();
 this.localStorage = new _LocalStorage();
 this.ononline = new Object();
 this.onhashchange = new Object();
 this.devicePixelRatio = 0;
 this.oninput = new Object();
 this.ondragleave = new Object();
 this.console = new _Console();
 this.onunload = new Object();
 this.ondrop = new Object();
 this.ondragstart = new Object();
 this.onload = new Object();
 this.location = new _Location();
 this.scrollY = 0;
 this.scrollX = 0;
 this.onpopstate = new Object();
}
_Window.prototype = new Object();
_Window.prototype.scrollBy = function scrollBy(x,x1) { };
_Window.prototype.focus = function focus() { };
_Window.prototype.Image = function Image(width_,width_1) { return _HTMLImageElement; };
_Window.prototype.clearTimeout = function clearTimeout(timeout) { };
_Window.prototype.dispatchEvent = function dispatchEvent(event) { return false; };
_Window.prototype.scroll = function scroll(x,x1) { };
_Window.prototype.scrollTo = function scrollTo(x,x1) { };
_Window.prototype.blur = function blur() { };
_Window.prototype.close = function close() { };
_Window.prototype.atob = function atob(encoded) { return ""; };
_Window.prototype.confirm = function confirm(message) { return false; };
_Window.prototype.setTimeout = function setTimeout() { return 0; };
_Window.prototype.btoa = function btoa(data) { return ""; };
_Window.prototype.postMessage = function postMessage(message,message1) { };
_Window.prototype.getComputedStyle = function getComputedStyle(node,node1) { return Element_prototype_style; };
_Window.prototype.clearInterval = function clearInterval(interval) { };
_Window.prototype.alert = function alert(message) { };
_Window.prototype.setInterval = function setInterval() { return 0; };
_Window.prototype.getSelection = function getSelection() { return _Selection; };
_Window.prototype.removeEventListener = function removeEventListener() { };
_Window.prototype.prompt = function prompt(message,message1) { return ""; };
_Window.prototype.addEventListener = function addEventListener() { };

window = new _Window();

