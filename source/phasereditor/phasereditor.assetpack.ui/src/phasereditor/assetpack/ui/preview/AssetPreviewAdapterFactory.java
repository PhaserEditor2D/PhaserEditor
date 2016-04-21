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
package phasereditor.assetpack.ui.preview;

import static phasereditor.ui.PhaserEditorUI.pickFileWithoutExtension;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

import phasereditor.assetpack.core.AssetGroupModel;
import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetModelFactory;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.AssetType;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AudioAssetModel;
import phasereditor.assetpack.core.AudioSpriteAssetModel;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.core.IAssetElementModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.PhysicsAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.TilemapAssetModel;
import phasereditor.assetpack.core.VideoAssetModel;
import phasereditor.audiosprite.ui.GdxMusicControl;
import phasereditor.ui.views.IPreviewFactory;

public class AssetPreviewAdapterFactory implements IAdapterFactory {

	private static final Class<?>[] ADAPTERS = { IPreviewFactory.class };

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Object adaptable, Class adapterType) {
		if (adaptable instanceof AssetModel) {
			AssetType type = ((AssetModel) adaptable).getType();
			switch (type) {
			case image:
				return createImagePreviewAdapter();
			case spritesheet:
				return createSpritesheetPreviewAdapter();
			case audio:
				return createAudioPreviewAdpter();
			case video:
				return createVideoPreviewAdpter();
			case audiosprite:
				return createAudioSpritePreviewAdpter();
			case atlas:
				return createAtlasPreviewAdapter();
			case tilemap:
				return createTilemapPreviewAdapter();
			case bitmapFont:
				return createBitmapFontPreviewAdapter();
			case physics:
				return createPhysicsPreviewAdapter();
			default:
				break;
			}
		}
		return null;
	}

	private static abstract class AssetModelPreviewFactory implements IPreviewFactory {

		public AssetModelPreviewFactory() {
		}

		@Override
		public final void updateControl(Control preview, Object element) {
			if (element instanceof IAssetElementModel) {
				AssetModel asset = ((IAssetElementModel) element).getAsset();
				updateControl2(preview, asset);
				selectInControl(preview, element);
			} else {
				updateControl2(preview, element);
			}
		}

		@Override
		public void selectInControl(Control preview, Object element) {
			// nothing by default
		}

		protected abstract void updateControl2(Control preview, Object toUpdate);

		@Override
		public String getTitle(Object element) {
			AssetModel asset;
			String title = "";
			if (element instanceof IAssetElementModel) {
				IAssetElementModel assetElem = (IAssetElementModel) element;
				title = " (" + assetElem.getName() + ")";
				asset = assetElem.getAsset();

			} else {
				asset = (AssetModel) element;
			}
			title = asset.getKey() + title;
			return title;
		}

		@Override
		public void hiddenControl(Control preview) {
			// nothing
		}

		@Override
		public IPersistableElement getPersistable(Object elem) {
			Object toPersist = elem;
			return new IPersistableElement() {

				@Override
				public void saveState(IMemento memento) {
					AssetPackModel pack = null;
					if (toPersist instanceof AssetModel) {
						pack = ((AssetModel) toPersist).getPack();
					} else if (toPersist instanceof AssetSectionModel) {
						pack = ((AssetSectionModel) toPersist).getPack();
					} else if (toPersist instanceof AssetGroupModel) {
						pack = ((AssetGroupModel) toPersist).getSection().getPack();
					} else if (toPersist instanceof IAssetElementModel) {
						pack = ((IAssetElementModel) toPersist).getAsset().getPack();
					}

					if (pack != null) {
						pack.saveState(memento, toPersist);
					}
				}

				@Override
				public String getFactoryId() {
					return AssetModelFactory.FACTORY_ID;
				}
			};
		}
	}

	private static IPreviewFactory createPhysicsPreviewAdapter() {
		return new AssetModelPreviewFactory() {

			@Override
			public void updateControl2(Control preview, Object element) {
				((PhysicsAssetPreviewComp) preview).setModel((PhysicsAssetModel) element);
			}

			@Override
			public void selectInControl(Control preview, Object element) {
				((PhysicsAssetPreviewComp) preview).selectElement(element);
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new PhysicsAssetPreviewComp(previewContainer, SWT.NONE);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof PhysicsAssetPreviewComp;
			}

		};
	}

	private static IPreviewFactory createBitmapFontPreviewAdapter() {
		return new AssetModelPreviewFactory() {

			@Override
			public void updateControl2(Control preview, Object element) {
				((BitmapFontAssetPreviewComp) preview).setModel((BitmapFontAssetModel) element);
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new BitmapFontAssetPreviewComp(previewContainer, SWT.NONE);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof BitmapFontAssetPreviewComp;
			}
		};
	}

	private static IPreviewFactory createTilemapPreviewAdapter() {
		return new AssetModelPreviewFactory() {

			@Override
			public void updateControl2(Control preview, Object element) {
				((TilemapAssetPreviewComp) preview).setModel((TilemapAssetModel) element);
			}

			@Override
			public void selectInControl(Control preview, Object element) {
				((TilemapAssetPreviewComp) preview).selectElement(element);
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new TilemapAssetPreviewComp(previewContainer, SWT.NONE);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof TilemapAssetPreviewComp;
			}
		};
	}

	private static IPreviewFactory createAtlasPreviewAdapter() {
		return new AssetModelPreviewFactory() {

			@Override
			public void updateControl2(Control preview, Object element) {
				AtlasAssetPreviewComp comp = (AtlasAssetPreviewComp) preview;
				comp.setModel((AtlasAssetModel) element);
			}

			@Override
			public void selectInControl(Control preview, Object element) {
				AtlasAssetPreviewComp comp = (AtlasAssetPreviewComp) preview;
				comp.selectElement(element);
				comp.getAtlasCanvas().setSingleFrame(true);
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new AtlasAssetPreviewComp(previewContainer, SWT.NONE);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof AtlasAssetPreviewComp
						&& !((AtlasAssetPreviewComp) c).getAtlasCanvas().isSingleFrame();
			}
		};
	}

	private static IPreviewFactory createAudioPreviewAdpter() {
		return new AssetModelPreviewFactory() {

			@Override
			public void updateControl2(Control preview, Object element) {
				GdxMusicControl comp = (GdxMusicControl) preview;
				AudioAssetModel model = (AudioAssetModel) element;
				IFile file = pickFileWithoutExtension(model.getFilesFromUrls(model.getUrls()), "mp3", "ogg");
				comp.load(file);
			}

			@Override
			public void hiddenControl(Control preview) {
				GdxMusicControl control = (GdxMusicControl) preview;
				control.stop();
				control.disposeMusic();
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new GdxMusicControl(previewContainer, SWT.NONE);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof GdxMusicControl;
			}
		};
	}

	private static IPreviewFactory createVideoPreviewAdpter() {
		return new AssetModelPreviewFactory() {

			@Override
			public void updateControl2(Control preview, Object element) {
				VideoPreviewComp comp = (VideoPreviewComp) preview;
				VideoAssetModel model = (VideoAssetModel) element;
				comp.setModel(model);
			}

			@Override
			public void hiddenControl(Control preview) {
				VideoPreviewComp comp = (VideoPreviewComp) preview;
				comp.setVideoFile(null);
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new VideoPreviewComp(previewContainer, SWT.NONE);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof VideoPreviewComp;
			}
		};
	}

	private static IPreviewFactory createAudioSpritePreviewAdpter() {
		return new AssetModelPreviewFactory() {

			@Override
			public void updateControl2(Control preview, Object element) {
				AudioSpriteAssetPreviewComp comp = (AudioSpriteAssetPreviewComp) preview;
				comp.setModel((AudioSpriteAssetModel) element);
			}

			@Override
			public void selectInControl(Control preview, Object element) {
				AudioSpriteAssetPreviewComp comp = (AudioSpriteAssetPreviewComp) preview;
				comp.selectElement(element);
			}

			@Override
			public void hiddenControl(Control preview) {
				AudioSpriteAssetPreviewComp comp = (AudioSpriteAssetPreviewComp) preview;
				comp.disposeMusicControl();
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new AudioSpriteAssetPreviewComp(previewContainer, SWT.NONE);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof AudioSpriteAssetPreviewComp;
			}
		};
	}

	private static IPreviewFactory createSpritesheetPreviewAdapter() {
		return new AssetModelPreviewFactory() {

			@Override
			public void updateControl2(Control preview, Object element) {
				SpritesheetAssetPreviewComp comp = (SpritesheetAssetPreviewComp) preview;
				SpritesheetAssetModel asset = (SpritesheetAssetModel) element;
				comp.setModel(asset);
			}

			@Override
			public void selectInControl(Control preview, Object element) {
				SpritesheetAssetPreviewComp comp = (SpritesheetAssetPreviewComp) preview;
				SpritesheetAssetModel.FrameModel frame = (SpritesheetAssetModel.FrameModel) element;
				comp.justShowThisFrame(frame);
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new SpritesheetAssetPreviewComp(previewContainer, SWT.NONE);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof SpritesheetAssetPreviewComp
						&& !((SpritesheetAssetPreviewComp) c).isJustOneFrameMode();
			}

			@Override
			public void hiddenControl(Control preview) {
				((SpritesheetAssetPreviewComp) preview).stopAnimation();
			}
		};
	}

	private static IPreviewFactory createImagePreviewAdapter() {
		return new AssetModelPreviewFactory() {
			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof ImageAssetPreviewComp;
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new ImageAssetPreviewComp(previewContainer, SWT.NONE);
			}

			@Override
			public void updateControl2(Control preview, Object element) {
				((ImageAssetPreviewComp) preview).setModel((ImageAssetModel) element);
			}
		};
	}

	@Override
	public Class<?>[] getAdapterList() {
		return ADAPTERS;
	}
}
