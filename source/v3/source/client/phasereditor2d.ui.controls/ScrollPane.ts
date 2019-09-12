namespace phasereditor2d.ui.controls {

    const SCROLL_BAR_WIDTH = 15;

    export class ScrollPane extends Control {

        private _clientControl: Control;
        private _scrollBar: HTMLDivElement;
        private _scrollHandler: HTMLDivElement;
        private _clientContentHeight: number = 0;

        constructor(clientControl: Control) {
            super("ScrollPane");

            this._clientControl = clientControl;
            this.add(this._clientControl);

            this._scrollBar = document.createElement("div");
            this._scrollBar.classList.add("ScrollBar");
            this.getElement().appendChild(this._scrollBar);

            this._scrollHandler = document.createElement("div");
            this._scrollHandler.classList.add("ScrollHandler");

            this.getElement().appendChild(this._scrollHandler);

            const l2 = (e: MouseEvent) => this.onMouseDown(e);
            const l3 = (e: MouseEvent) => this.onMouseUp(e);
            const l4 = (e: MouseEvent) => this.onMouseMove(e);
            const l5 = (e: MouseEvent) => {
                if (!this.getElement().isConnected) {
                    window.removeEventListener("mousedown", l2);
                    window.removeEventListener("mouseup", l3);
                    window.removeEventListener("mousemove", l4);
                    window.removeEventListener("mousemove", l5);
                }
            };

            window.addEventListener("mousedown", l2);
            window.addEventListener("mouseup", l3);
            window.addEventListener("mousemove", l4);
            window.addEventListener("mousemove", l5);

            this._clientControl.getElement().addEventListener("wheel", e => this.onClientWheel(e));
            this._scrollBar.addEventListener("mousedown", e => this.onBarMouseDown(e))
        }

        updateScroll(clientContentHeight: number) {
            const scrollY = this._clientControl.getScrollY();
            const b = this.getBounds();
            let newScrollY = scrollY;
            newScrollY = Math.max(-this._clientContentHeight + b.height, newScrollY);
            newScrollY = Math.min(0, newScrollY);

            if (newScrollY != scrollY) {
                this._clientContentHeight = clientContentHeight;
                this.setClientScrollY(scrollY);
            } else if (clientContentHeight !== this._clientContentHeight) {
                this._clientContentHeight = clientContentHeight;
                this.layout();
            }
        }

        private onBarMouseDown(e: MouseEvent) {
            const b = this.getBounds();
            this.setClientScrollY(- e.offsetY / b.height * (this._clientContentHeight - b.height));
        }

        private onClientWheel(e: WheelEvent) {

            if (e.shiftKey || e.ctrlKey || e.metaKey || e.altKey) {
                return;
            }

            let y = this._clientControl.getScrollY();

            y += e.deltaY < 0 ? 30 : -30;

            this.setClientScrollY(y);
        }

        private setClientScrollY(y: number) {
            const b = this.getBounds();
            y = Math.max(-this._clientContentHeight + b.height, y);
            y = Math.min(0, y);

            this._clientControl.setScrollY(y);

            this.layout();
        }


        private _startDragY = -1;
        private _startScrollY = 0;


        private onMouseDown(e: MouseEvent) {
            if (e.target === this._scrollHandler) {
                e.stopImmediatePropagation();
                this._startDragY = e.y;
                this._startScrollY = this._clientControl.getScrollY();
            }
        }

        private onMouseMove(e: MouseEvent) {
            if (this._startDragY !== -1) {
                let delta = e.y - this._startDragY;
                const b = this.getBounds();
                delta = delta / b.height * this._clientContentHeight;
                this.setClientScrollY(this._startScrollY - delta);
            }
        }

        private onMouseUp(e: MouseEvent) {
            if (this._startDragY !== -1) {
                e.stopImmediatePropagation();
                this._startDragY = -1;
            }
        }

        private setClientBounds(x: number, y: number, w: number, h: number): void {
            const b = this._clientControl.getBounds();
            if (b.width != w || b.height != h) {
                this._clientControl.setBoundsValues(x, y, w, h);
            }
        }

        layout(): void {
            const b = this.getBounds();

            controls.setElementBounds(this.getElement(), b);

            if (b.height < this._clientContentHeight) {

                this.setClientBounds(0, 0, b.width - SCROLL_BAR_WIDTH, b.height);

                // scroll bar

                this._scrollBar.style.display = "inherit";

                controls.setElementBounds(this._scrollBar, {
                    x: b.width - SCROLL_BAR_WIDTH,
                    y: 0,
                    width: SCROLL_BAR_WIDTH - 2,
                    height: b.height
                });

                // handler

                this._scrollHandler.style.display = "inherit";
                const h = Math.max(10, b.height / this._clientContentHeight * b.height);
                const y = -(b.height - h) * this._clientControl.getScrollY() / (this._clientContentHeight - b.height);

                controls.setElementBounds(this._scrollHandler, {
                    x: b.width - SCROLL_BAR_WIDTH,
                    y: y,
                    width: SCROLL_BAR_WIDTH - 1,
                    height: h
                });

            } else {

                this.setClientBounds(0, 0, b.width, b.height);

                this._scrollBar.style.display = "none";
                this._scrollHandler.style.display = "none";
            }


        }
    }

}