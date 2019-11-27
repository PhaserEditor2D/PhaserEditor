/// <reference path="./MonacoEditor.ts" />

namespace phasereditor2d.code.ui.editors {

    export class JavaScriptEditorFactory extends MonacoEditorFactory {

        constructor() {
            super("javascript", webContentTypes.core.CONTENT_TYPE_JAVASCRIPT);
        }

        createEditor(): colibri.ui.ide.EditorPart {
            return new JavaScriptEditor();
        }
    }

    export class JavaScriptEditor extends MonacoEditor {

        constructor() {
            super("javascript");
        }

        createPart() {

            super.createPart();

            const editor = this.getMonacoEditor();


            editor.onDidChangeCursorPosition(e => {

                const model = editor.getModel();

                const str = getStringTokenValue(model, e.position);

                if (str) {

                    this.setSelection([str]);

                    // const obj = pack.core.PackFinder.findPackItemOrFrameWithKey(str);

                    // console.log(obj);
                }

            });
        }
    }

    function getStringTokenValue(model: monaco.editor.ITextModel, pos: monaco.IPosition) {

        const input = model.getLineContent(pos.lineNumber);

        const cursor = pos.column - 1;

        let i = 0;
        let tokenOffset = 0;
        let openChar = "";

        while (i < input.length) {

            const c = input[i];

            if (openChar === c) {

                // end string token

                if (cursor >= tokenOffset && cursor <= i) {

                    return input.slice(tokenOffset, i);
                }

                openChar = "";

            } else if (c === "'" || c === '"') {

                // start string token

                openChar = c;

                tokenOffset = i + 1;
            }

            i++;
        }
    }
}