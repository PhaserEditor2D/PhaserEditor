namespace phasereditor2d.ui.ide.editors.pack.parsers {

    export class UnityAtlasParser extends AbstractAtlasParser {

        protected parse2(imageFrames: ImageFrame[], image: controls.IImage, atlas: string) {

            // Taken from Phaser code.

            const data = atlas.split('\n');

            const lineRegExp = /^[ ]*(- )*(\w+)+[: ]+(.*)/;

            let prevSprite = '';
            let currentSprite = '';
            let rect = { x: 0, y: 0, width: 0, height: 0 };

            // const pivot = { x: 0, y: 0 };
            // const border = { x: 0, y: 0, z: 0, w: 0 };

            for (let i = 0; i < data.length; i++) {
                const results = data[i].match(lineRegExp);

                if (!results) {
                    continue;
                }

                const isList = (results[1] === '- ');
                const key = results[2];
                const value = results[3];

                if (isList) {
                    if (currentSprite !== prevSprite) {
                        this.addFrame(image, imageFrames, currentSprite, rect);
                        prevSprite = currentSprite;
                    }

                    rect = { x: 0, y: 0, width: 0, height: 0 };
                }

                if (key === 'name') {
                    //  Start new list
                    currentSprite = value;
                    continue;
                }

                switch (key) {
                    case 'x':
                    case 'y':
                    case 'width':
                    case 'height':
                        rect[key] = parseInt(value, 10);
                        break;

                    // case 'pivot':
                    //     pivot = eval('const obj = ' + value);
                    //     break;

                    // case 'border':
                    //     border = eval('const obj = ' + value);
                    //     break;
                }
            }

            if (currentSprite !== prevSprite) {
                this.addFrame(image, imageFrames, currentSprite, rect);
            }

        }

        private addFrame(image: controls.IImage, imageFrames: ImageFrame[], spriteName: string, rect: any) {
            const src = new controls.Rect(rect.x, rect.y, rect.width, rect.height);
            src.y = image.getHeight() - src.y - src.h;
            const dst = new controls.Rect(0, 0, rect.width, rect.height);
            const srcSize = new controls.Point(rect.width, rect.height);
            const fd = new FrameData(imageFrames.length, src, dst, srcSize);
            imageFrames.push(new ImageFrame(spriteName, image, fd));
        }

    }
}

/*

TextureImporter:
  spritePivot: {x: .5, y: .5}
  spriteBorder: {x: 0, y: 0, z: 0, w: 0}
  spritePixelsToUnits: 100
  spriteSheet:
    sprites:
    - name: asteroids_0
      rect:
        serializedVersion: 2
        x: 5
        y: 328
        width: 65
        height: 82
      alignment: 0
      pivot: {x: 0, y: 0}
      border: {x: 0, y: 0, z: 0, w: 0}
    - name: asteroids_1
      rect:
        serializedVersion: 2
        x: 80
        y: 322
        width: 53
        height: 88
      alignment: 0
      pivot: {x: 0, y: 0}
      border: {x: 0, y: 0, z: 0, w: 0}
  spritePackingTag: Asteroids

  */