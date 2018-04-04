// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.atlas.core;

import org.json.JSONObject;

import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.tools.texturepacker.TexturePacker.Settings;

public class SettingsBean extends Settings implements Cloneable {

	public SettingsBean() {
		init();
	}

	public SettingsBean(SettingsBean settings) {
		super(settings);
		init();
	}

	private void init() {
		stripWhitespaceX = true;
		stripWhitespaceY = true;
		pot = false;
		limitMemory = false;
		flattenPaths = false;
		useIndexes = false;
	}

	public void update(SettingsBean settings) {
		fast = settings.fast;
		rotation = settings.rotation;
		pot = settings.pot;
		minWidth = settings.minWidth;
		minHeight = settings.minHeight;
		maxWidth = settings.maxWidth;
		maxHeight = settings.maxHeight;
		paddingX = settings.paddingX;
		paddingY = settings.paddingY;
		edgePadding = settings.edgePadding;
		duplicatePadding = settings.duplicatePadding;
		alphaThreshold = settings.alphaThreshold;
		ignoreBlankImages = settings.ignoreBlankImages;
		stripWhitespaceX = settings.stripWhitespaceX;
		stripWhitespaceY = settings.stripWhitespaceY;
		alias = settings.alias;
		format = settings.format;
		jpegQuality = settings.jpegQuality;
		outputFormat = settings.outputFormat;
		filterMin = settings.filterMin;
		filterMag = settings.filterMag;
		wrapX = settings.wrapX;
		wrapY = settings.wrapY;
		debug = settings.debug;
		silent = settings.silent;
		combineSubdirectories = settings.combineSubdirectories;
		flattenPaths = settings.flattenPaths;
		premultiplyAlpha = settings.premultiplyAlpha;
		square = settings.square;
		useIndexes = settings.useIndexes;
		bleed = settings.bleed;
		limitMemory = settings.limitMemory;
		grid = settings.grid;
		scale = settings.scale;
		scaleSuffix = settings.scaleSuffix;
		atlasExtension = settings.atlasExtension;
	}

	@Override
	public SettingsBean clone() {
		try {
			return (SettingsBean) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	public void read(JSONObject obj) {
		pot = obj.optBoolean("pot", true);
		paddingX = obj.optInt("paddingX", 2);
		paddingY = obj.optInt("paddingY", 2);
		edgePadding = obj.optBoolean("edgePadding", true);
		duplicatePadding = obj.optBoolean("duplicatePadding", false);
		rotation = obj.optBoolean("rotation", false);
		minWidth = obj.optInt("minWidth", 16);
		minHeight = obj.optInt("minHeight", 16);
		maxWidth = obj.optInt("maxWidth", 1024);
		maxHeight = obj.optInt("maxHeight", 1024);
		square = obj.optBoolean("square", false);
		stripWhitespaceX = obj.optBoolean("stripWhitespaceX", false);
		stripWhitespaceY = obj.optBoolean("stripWhitespaceY", false);
		alphaThreshold = obj.optInt("alphaThreshold", 0);
		filterMin = TextureFilter.valueOf(obj.optString("filterMin", "Nearest"));
		filterMag = TextureFilter.valueOf(obj.optString("filterMag", "Nearest"));
		wrapX = TextureWrap.valueOf(obj.optString("wrapX", "ClampToEdge"));
		wrapY = TextureWrap.valueOf(obj.optString("wrapY", "ClampToEdge"));
		format = Format.valueOf(obj.optString("format", "RGBA8888"));
		alias = obj.optBoolean("alias", true);
		outputFormat = obj.optString("outputFormat", "png");
		jpegQuality = (float) obj.optDouble("jpegQuality", 0.9);
		ignoreBlankImages = obj.optBoolean("ignoreBlankImages", true);
		fast = obj.optBoolean("fast", false);
		debug = obj.optBoolean("debug", false);
		silent = obj.optBoolean("silent", false);
		combineSubdirectories = obj.optBoolean("combineSubdirectories", false);
		flattenPaths = obj.optBoolean("flattenPaths", false);
		premultiplyAlpha = obj.optBoolean("premultiplyAlpha", false);
		useIndexes = obj.optBoolean("useIndexes", true);
		bleed = obj.optBoolean("bleed", true);
		limitMemory = obj.optBoolean("limitMemory", true);
		grid = obj.optBoolean("grid", false);
		// avoid scale
		// avoid scaleSuffix
		atlasExtension = obj.optString("atlasExtension", ".atlas");
	}

	public void write(JSONObject obj) {
		obj.put("pot", pot);
		obj.put("paddingX", paddingX);
		obj.put("paddingY", paddingY);
		obj.put("edgePadding", edgePadding);
		obj.put("duplicatePadding", duplicatePadding);
		obj.put("rotation", rotation);
		obj.put("minWidth", minWidth);
		obj.put("minHeight", minHeight);
		obj.put("maxWidth", maxWidth);
		obj.put("maxHeight", maxHeight);
		obj.put("square", square);
		obj.put("stripWhitespaceX", stripWhitespaceX);
		obj.put("stripWhitespaceY", stripWhitespaceY);
		obj.put("alphaThreshold", alphaThreshold);
		obj.put("filterMin", filterMin.name());
		obj.put("filterMag", filterMag.name());
		obj.put("wrapX", wrapX.name());
		obj.put("wrapY", wrapY.name());
		obj.put("format", format.name());
		obj.put("alias", alias);
		obj.put("outputFormat", outputFormat);
		obj.put("jpegQuality", jpegQuality);
		obj.put("ignoreBlankImages", ignoreBlankImages);
		obj.put("fast", fast);
		obj.put("debug", debug);
		obj.put("silent", silent);
		obj.put("combineSubdirectories", combineSubdirectories);
		obj.put("flattenPaths", flattenPaths);
		obj.put("premultiplyAlpha", premultiplyAlpha);
		obj.put("useIndexes", useIndexes);
		obj.put("bleed", bleed);
		obj.put("limitMemory", limitMemory);
		obj.put("grid", grid);
		// avoid scale
		// avoid scaleSuffix
		obj.put("atlasExtension", atlasExtension);
	}

	public boolean isPot() {
		return pot;
	}

	public void setPot(boolean pot) {
		this.pot = pot;
	}

	public int getPaddingX() {
		return paddingX;
	}

	public void setPaddingX(int paddingX) {
		this.paddingX = paddingX;
	}

	public int getPaddingY() {
		return paddingY;
	}

	public void setPaddingY(int paddingY) {
		this.paddingY = paddingY;
	}

	public boolean isEdgePadding() {
		return edgePadding;
	}

	public void setEdgePadding(boolean edgePadding) {
		this.edgePadding = edgePadding;
	}

	public boolean isDuplicatePadding() {
		return duplicatePadding;
	}

	public void setDuplicatePadding(boolean duplicatePadding) {
		this.duplicatePadding = duplicatePadding;
	}

	public boolean isRotation() {
		return rotation;
	}

	public void setRotation(boolean rotation) {
		this.rotation = rotation;
	}

	public int getMinWidth() {
		return minWidth;
	}

	public void setMinWidth(int minWidth) {
		this.minWidth = minWidth;
	}

	public int getMinHeight() {
		return minHeight;
	}

	public void setMinHeight(int minHeight) {
		this.minHeight = minHeight;
	}

	public int getMaxWidth() {
		return maxWidth;
	}

	public void setMaxWidth(int maxWidth) {
		this.maxWidth = maxWidth;
	}

	public int getMaxHeight() {
		return maxHeight;
	}

	public void setMaxHeight(int maxHeight) {
		this.maxHeight = maxHeight;
	}

	public boolean isSquare() {
		return square;
	}

	public void setSquare(boolean square) {
		this.square = square;
	}

	public boolean isStripWhitespaceX() {
		return stripWhitespaceX;
	}

	public void setStripWhitespaceX(boolean stripWhitespaceX) {
		this.stripWhitespaceX = stripWhitespaceX;
	}

	public boolean isStripWhitespaceY() {
		return stripWhitespaceY;
	}

	public void setStripWhitespaceY(boolean stripWhitespaceY) {
		this.stripWhitespaceY = stripWhitespaceY;
	}

	public int getAlphaThreshold() {
		return alphaThreshold;
	}

	public void setAlphaThreshold(int alphaThreshold) {
		this.alphaThreshold = alphaThreshold;
	}

	public TextureFilter getFilterMin() {
		return filterMin;
	}

	public void setFilterMin(TextureFilter filterMin) {
		this.filterMin = filterMin;
	}

	public TextureFilter getFilterMag() {
		return filterMag;
	}

	public void setFilterMag(TextureFilter filterMag) {
		this.filterMag = filterMag;
	}

	public TextureWrap getWrapX() {
		return wrapX;
	}

	public void setWrapX(TextureWrap wrapX) {
		this.wrapX = wrapX;
	}

	public TextureWrap getWrapY() {
		return wrapY;
	}

	public void setWrapY(TextureWrap wrapY) {
		this.wrapY = wrapY;
	}

	public Format getFormat() {
		return format;
	}

	public void setFormat(Format format) {
		this.format = format;
	}

	public boolean isAlias() {
		return alias;
	}

	public void setAlias(boolean alias) {
		this.alias = alias;
	}

	public String getOutputFormat() {
		return outputFormat;
	}

	public void setOutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
	}

	public float getJpegQuality() {
		return jpegQuality;
	}

	public void setJpegQuality(float jpegQuality) {
		this.jpegQuality = jpegQuality;
	}

	public boolean isIgnoreBlankImages() {
		return ignoreBlankImages;
	}

	public void setIgnoreBlankImages(boolean ignoreBlankImages) {
		this.ignoreBlankImages = ignoreBlankImages;
	}

	public boolean isFast() {
		return fast;
	}

	public void setFast(boolean fast) {
		this.fast = fast;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public boolean isSilent() {
		return silent;
	}

	public void setSilent(boolean silent) {
		this.silent = silent;
	}

	public boolean isCombineSubdirectories() {
		return combineSubdirectories;
	}

	public void setCombineSubdirectories(boolean combineSubdirectories) {
		this.combineSubdirectories = combineSubdirectories;
	}

	public boolean isFlattenPaths() {
		return flattenPaths;
	}

	public void setFlattenPaths(boolean flattenPaths) {
		this.flattenPaths = flattenPaths;
	}

	public boolean isPremultiplyAlpha() {
		return premultiplyAlpha;
	}

	public void setPremultiplyAlpha(boolean premultiplyAlpha) {
		this.premultiplyAlpha = premultiplyAlpha;
	}

	public boolean isUseIndexes() {
		return useIndexes;
	}

	public void setUseIndexes(boolean useIndexes) {
		this.useIndexes = useIndexes;
	}

	public boolean isBleed() {
		return bleed;
	}

	public void setBleed(boolean bleed) {
		this.bleed = bleed;
	}

	public boolean isLimitMemory() {
		return limitMemory;
	}

	public void setLimitMemory(boolean limitMemory) {
		this.limitMemory = limitMemory;
	}

	public boolean isGrid() {
		return grid;
	}

	public void setGrid(boolean grid) {
		this.grid = grid;
	}

	public float[] getScale() {
		return scale;
	}

	public void setScale(float[] scale) {
		this.scale = scale;
	}

	public String[] getScaleSuffix() {
		return scaleSuffix;
	}

	public void setScaleSuffix(String[] scaleSuffix) {
		this.scaleSuffix = scaleSuffix;
	}

	public String getAtlasExtension() {
		return atlasExtension;
	}

	public void setAtlasExtension(String atlasExtension) {
		this.atlasExtension = atlasExtension;
	}

}