/// <reference path="./AbstractAtlasParser.ts" />

namespace phasereditor2d.ui.ide.editors.pack.parsers {

    export class AtlasXMLParser extends AbstractAtlasParser {

        constructor(packItem: AssetPackItem) {
            super(packItem);
        }

        protected parse2(imageFrames: ImageFrame[], image: controls.IImage, atlas: string) {
            try {
                const parser = new DOMParser();
                const data = parser.parseFromString(atlas, "text/xml");
                const elements = data.getElementsByTagName("SubTexture");

                for (let i = 0; i < elements.length; i++) {
                    const elem = elements.item(i);

                    const name = elem.getAttribute("name");

                    const frameX = Number.parseInt(elem.getAttribute("x"));
                    const frameY = Number.parseInt(elem.getAttribute("y"));
                    const frameW = Number.parseInt(elem.getAttribute("width"));
                    const frameH = Number.parseInt(elem.getAttribute("height"));

                    let spriteX = frameX;
                    let spriteY = frameY;
                    let spriteW = frameW;
                    let spriteH = frameH;

                    if (elem.hasAttribute("frameX")) {
                        spriteX = Number.parseInt(elem.getAttribute("frameX"));
                        spriteY = Number.parseInt(elem.getAttribute("frameY"));
                        spriteW = Number.parseInt(elem.getAttribute("frameWidth"));
                        spriteH = Number.parseInt(elem.getAttribute("frameHeight"));
                    }

                    const fd = new FrameData(i,
                        new controls.Rect(frameX, frameY, frameW, frameH),
                        new controls.Rect(spriteX, spriteY, spriteW, spriteH),
                        new controls.Point(frameW, frameH)
                    );
                    imageFrames.push(new ImageFrame(name, image, fd));
                }
            } catch (e) {
                console.error(e);
            }
        }
    }
}