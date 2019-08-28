namespace phasereditor2d.ui.ide {

    export class ViewPart extends Part {

        constructor(id: string) {
            super(id);
            this.getElement().classList.add("view");
        }
    }

}