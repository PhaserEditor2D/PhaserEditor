namespace phasereditor2d.ui.controls {

    const ROW_HEIGHT = 20;
    const FONT_HEIGHT = 14;
    const FONT_OFFSET = 2;
    const ACTION_WIDTH = 20;
    const PANEL_BORDER_SIZE = 4;
    const SPLIT_OVER_ZONE_WIDTH = 6;

    export declare type Bounds = {
        x?: number,
        y?: number,
        width?: number,
        height?: number
    }
    export function setElementBounds(elem: HTMLElement, bounds: Bounds) {
        elem.style.left = bounds.x + "px";
        elem.style.top = bounds.y + "px";
        elem.style.width = bounds.width + "px";
        elem.style.height = bounds.height + "px";
    }

    export function getElementBounds(elem: HTMLElement): Bounds {
        return {
            x: elem.clientLeft,
            y: elem.clientTop,
            width: elem.clientWidth,
            height: elem.clientHeight
        };
    }
    export class Control {
        private _bounds: Bounds = { x: 0, y: 0, width: 0, height: 0 };
        private _element: HTMLElement;
        private _children: Control[];

        constructor(tagName: string = "div") {
            this._children = [];
            this._element = document.createElement(tagName);
            this.addClass("control");
        }

        addClass(...tokens: string[]): void {
            for (let token of tokens) {
                this._element.classList.add();
            }
        }

        getElement() {
            return this._element;
        }

        getControlPosition(windowX: number, windowY: number) {
            const b = this.getElement().getBoundingClientRect();
            return {
                x: windowX - b.left,
                y: windowY - b.top
            };
        }

        containsLocalPoint(x: number, y: number) {
            return x >= 0 && x <= this._bounds.width && y >= 0 && y <= this._bounds.height;
        }

        setBounds(bounds: Bounds): void {
            if (bounds.x !== undefined) {
                this._bounds.x = bounds.x;
            }

            this._bounds.x = bounds.x === undefined ? this._bounds.x : bounds.x;
            this._bounds.y = bounds.y === undefined ? this._bounds.y : bounds.y;
            this._bounds.width = bounds.width === undefined ? this._bounds.width : bounds.width;
            this._bounds.height = bounds.height === undefined ? this._bounds.height : bounds.height;

            this.layout();
        }

        setBoundsValues(x: number, y: number, w: number, h: number): void {
            this.setBounds({ x: x, y: y, width: w, height: h });
        }

        getBounds() {
            return this._bounds;
        }

        setLocation(x: number, y: number): void {
            this._element.style.left = x + "px";
            this._element.style.top = y + "px";
            this._bounds.x = x;
            this._bounds.y = y;
        }

        layout(): void {
            setElementBounds(this._element, this._bounds);
            for (let child of this._children) {
                child.layout();
            }
        }

        add(control: Control) : void {
            this._children.push(control);
            this._element.appendChild(control.getElement());
        }

        getChildren() {
            return this._children;
        }
    }

    class PanelTitle extends Control {
        private _textControl: Control;
        private _toolbar: PanelToolbar;


        constructor() {
            super();

            this.getElement().classList.add("panelTitle");

            this._textControl = new Control();
            this.add(this._textControl);

            this._toolbar = new PanelToolbar();
            this.add(this._toolbar);
        }

        setText(text: string) {
            this._textControl.getElement().innerHTML = text;
        }

        getToolbar(): IToolbar {
            return this._toolbar;
        }

        layout() {
            super.layout();

            const b = this.getBounds();

            const elem = this._textControl.getElement();
            elem.style.top = FONT_OFFSET + "px";
            elem.style.left = FONT_OFFSET * 2 + "px";

            const toolbarWidth = this._toolbar.getActions().length * ACTION_WIDTH;
            this._toolbar.setBoundsValues(b.width - toolbarWidth, 0, toolbarWidth, ROW_HEIGHT);

        }
    }

    export class Action {

    }

    export class ActionButton extends Control {
        private _action: Action;

        constructor(action: Action) {
            super("button");

            this._action = action;

            this.getElement().classList.add("actionButton");
        }

        getAction() {
            return this._action;
        }

        layout() {
            super.layout();
        }
    }

    export interface IToolbar {
        addAction(action: Action);

        getActions(): Action[];
    }

    class PanelToolbar extends Control implements IToolbar {
        private _actions: Action[];
        private _buttons: ActionButton[];

        constructor() {
            super();

            this._actions = [];
            this._buttons = [];

            this.getElement().classList.add("panelToolbar");
        }

        addAction(action: Action) {
            this._actions.push(action);
            const b = new ActionButton(action);
            this._buttons.push(b);
            this.add(b);
        }

        getActions() {
            return this._actions;
        }

        layout() {
            super.layout();

            const b = this.getBounds();

            for (let i = 0; i < this._buttons.length; i++) {
                const btn = this._buttons[i];
                btn.setBoundsValues(i * ACTION_WIDTH, 0, ACTION_WIDTH, b.height);
            }
        }
    }

    export class Panel extends Control {
        private _clientAreaElement: HTMLDivElement;
        private _cornerElements: HTMLDivElement[] = [null, null, null, null];
        private _panelTitle: PanelTitle;
        private _title: string;

        constructor(hasTitle: boolean = true) {
            super();

            this.getElement().classList.add("panel");

            for (let i = 0; i < 4; i++) {
                const elem = document.createElement("div");
                elem.classList.add("panelCorner");
                this.getElement().appendChild(elem);
                this._cornerElements[i] = elem;
            }

            if (hasTitle) {
                this._panelTitle = new PanelTitle();
                this.add(this._panelTitle);
            }

            this._clientAreaElement = document.createElement("div");
            this._clientAreaElement.classList.add("panelClientArea");

            this.getElement().appendChild(this._clientAreaElement);
        }

        setTitle(title: string) {
            this._title = title;
            this._panelTitle.setText(title);
        }

        getTitle() {
            return this._title;
        }

        getToolbar(): IToolbar {
            return this._panelTitle.getToolbar();
        }

        layout() {
            super.layout();

            const b = this.getBounds();

            const cornerSize = ROW_HEIGHT;

            setElementBounds(this._cornerElements[0], {
                x: 0,
                y: 0,
                width: cornerSize,
                height: cornerSize
            });

            setElementBounds(this._cornerElements[1], {
                x: b.width - cornerSize,
                y: 0,
                width: cornerSize,
                height: cornerSize
            });

            setElementBounds(this._cornerElements[2], {
                x: b.width - cornerSize,
                y: b.height - cornerSize,
                width: cornerSize,
                height: cornerSize
            });

            setElementBounds(this._cornerElements[3], {
                x: 0,
                y: b.height - cornerSize,
                width: cornerSize,
                height: cornerSize
            });

            if (this._panelTitle) {
                this._panelTitle.setBoundsValues(PANEL_BORDER_SIZE, PANEL_BORDER_SIZE, b.width - PANEL_BORDER_SIZE * 2, ROW_HEIGHT);

                setElementBounds(this._clientAreaElement, {
                    x: PANEL_BORDER_SIZE,
                    y: PANEL_BORDER_SIZE + ROW_HEIGHT,
                    width: b.width - PANEL_BORDER_SIZE * 2,
                    height: b.height - PANEL_BORDER_SIZE * 2 - ROW_HEIGHT
                });
            } else {
                setElementBounds(this._clientAreaElement, {
                    x: PANEL_BORDER_SIZE,
                    y: PANEL_BORDER_SIZE,
                    width: b.width - PANEL_BORDER_SIZE * 2,
                    height: b.height - PANEL_BORDER_SIZE * 2
                });
            }
        }
    }

    export class SplitPanel extends Control {
        private _leftControl: Control;
        private _rightControl: Control;
        private _horizontal: boolean;
        private _splitPosition: number;
        private _splitFactor: number;
        private _splitWidth: number;
        private _startDrag: number = -1;
        private _startPos: number;

        constructor(left?: Control, right?: Control, horizontal = true) {
            super();

            this.getElement().classList.add("split");

            this._horizontal = horizontal;
            this._splitPosition = 50;
            this._splitFactor = 0.5;
            this._splitWidth = 2;

            const l1 = (e: MouseEvent) => this.onMouseLeave(e);
            const l2 = (e: MouseEvent) => this.onMouseDown(e);
            const l3 = (e: MouseEvent) => this.onMouseUp(e);
            const l4 = (e: MouseEvent) => this.onMouseMove(e);
            const l5 = (e: MouseEvent) => {
                if (!this.getElement().isConnected) {
                    window.removeEventListener("mouseleave", l1);
                    window.removeEventListener("mousedown", l2);
                    window.removeEventListener("mouseup", l3);
                    window.removeEventListener("mousemove", l4);
                    window.removeEventListener("mousemove", l5);
                }
            };

            window.addEventListener("mouseleave", l1);
            window.addEventListener("mousedown", l2);
            window.addEventListener("mouseup", l3);
            window.addEventListener("mousemove", l4);
            window.addEventListener("mousemove", l5);

            if (left) {
                this.setLeftControl(left);
            }

            if (right) {
                this.setRightControl(right);
            }
        }

        private onMouseDown(e: MouseEvent) {
            const pos = this.getControlPosition(e.x, e.y);
            const offset = this._horizontal ? pos.x : pos.y;

            const inside = Math.abs(offset - this._splitPosition) <= SPLIT_OVER_ZONE_WIDTH && this.containsLocalPoint(pos.x, pos.y);

            if (inside) {
                e.preventDefault();
                this._startDrag = this._horizontal ? e.x : e.y;
                this._startPos = this._splitPosition;
            }
        }

        private onMouseUp(e: MouseEvent) {
            this._startDrag = -1;
        }

        private onMouseMove(e: MouseEvent) {
            const pos = this.getControlPosition(e.x, e.y);
            const offset = this._horizontal ? pos.x : pos.y;
            const screen = this._horizontal ? e.x : e.y;
            const boundsSize = this._horizontal ? this.getBounds().width : this.getBounds().height;
            const cursorResize = this._horizontal ? "ew-resize" : "ns-resize";

            const inside = Math.abs(offset - this._splitPosition) <= SPLIT_OVER_ZONE_WIDTH && this.containsLocalPoint(pos.x, pos.y);

            if (inside) {
                e.preventDefault();
                this.getElement().style.cursor = cursorResize;
            } else {
                this.getElement().style.cursor = "inherit";
            }

            if (this._startDrag !== -1) {
                this.getElement().style.cursor = cursorResize;
                const newPos = this._startPos + screen - this._startDrag;
                if (newPos > 100 && boundsSize - newPos > 100) {
                    this._splitPosition = newPos;
                    this._splitFactor = this._splitPosition / boundsSize;
                    this.layout();
                }
            }
        }

        private onMouseLeave(e: MouseEvent) {
            this.getElement().style.cursor = "inherit";
            this._startDrag = -1;
        }

        setHorizontal(horizontal: boolean = true) {
            this._horizontal = horizontal;
        }

        setVertical(vertical: boolean = true) {
            this._horizontal = !vertical;
        }

        getSplitFactor() {
            return this._splitFactor;
        }

        private getSize() {
            const b = this.getBounds();
            return this._horizontal ? b.width : b.height;
        }

        setSplitFactor(factor: number) {
            this._splitFactor = Math.min(Math.max(0, factor), 1);
            this._splitPosition = this.getSize() * this._splitFactor;
        }

        setLeftControl(control: Control) {
            this._leftControl = control;
            this.add(control);
        }

        getLeftControl() {
            return this._leftControl;
        }

        setRightControl(control: Control) {
            this._rightControl = control;
            this.add(control);
        }

        getRightControl() {
            return this._rightControl;
        }

        layout() {
            setElementBounds(this.getElement(), this.getBounds());

            if (!this._leftControl || !this._rightControl) {
                return;
            }

            this.setSplitFactor(this._splitFactor);

            const pos = this._splitPosition;
            const sw = this._splitWidth;
            let b = this.getBounds();

            if (this._horizontal) {
                this._leftControl.setBoundsValues(0, 0, pos - sw, b.height);
                this._rightControl.setBoundsValues(pos + sw, 0, b.width - pos - sw, b.height);
            } else {
                this._leftControl.setBoundsValues(0, 0, b.width, pos - sw);
                this._rightControl.setBoundsValues(0, pos + sw, b.width, b.height - pos - sw);
            }
        }
    }

    export class PaddingPane extends Control {
        private _padding: number;
        private _control: Control;

        constructor(control?: Control, padding: number = 5) {
            super();
            this._padding = padding;
            this.getElement().classList.add("paddingPane");
            this.setControl(control);
        }

        setControl(control: Control) {
            this._control = control;
            if (this._control) {
                this.add(control);
            }
        }

        getControl() {
            return this._control;
        }

        setPadding(padding: number) {
            this._padding = padding;
        }

        getPadding() {
            return this._padding;
        }

        layout() {
            const b = this.getBounds();

            setElementBounds(this.getElement(), b);

            if (this._control) {
                this._control.setBoundsValues(this._padding, this._padding, b.width - this._padding * 2, b.height - this._padding * 2);
            }
        }
    }

}