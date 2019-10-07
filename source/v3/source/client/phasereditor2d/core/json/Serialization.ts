namespace phasereditor2d.core.json {

    export function write(data: any, name: string, value: any, defaultValue?: any): void {
        if (value !== defaultValue) {
            data[name] = value;
        }
    }

    export function read(data: any, name: string, defaultValue?: any): any {

        if (name in data) {
            return data[name];
        }

        return defaultValue;
    }

}