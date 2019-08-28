namespace phasereditor2d.ui.controls {

    export const ROW_HEIGHT = 20;
    export const FONT_HEIGHT = 14;
    export const FONT_OFFSET = 2;
    export const ACTION_WIDTH = 20;
    export const PANEL_BORDER_SIZE = 4;
    export const SPLIT_OVER_ZONE_WIDTH = 6;

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
}