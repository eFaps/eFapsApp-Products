/*
 * Copyright 2003 - 2009 The eFaps Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */

package org.efaps.esjp.products;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;

import org.apache.sanselan.ImageFormat;
import org.apache.sanselan.ImageInfo;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.Sanselan;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractCommand;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.admin.ui.field.Field;
import org.efaps.db.Checkin;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.Update;
import org.efaps.util.EFapsException;

import com.mortennobel.imagescaling.DimensionConstrain;
import com.mortennobel.imagescaling.ResampleOp;

/**
 * TODO comment!
 * First fast draft for testing purpose only!!!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("ac49af4b-e10e-43f8-976d-8b5170dbfb7a")
@EFapsRevision("$Rev$")
public abstract class Image_Base
{
    public Return access4OriginalFieldUI(final Parameter _parameter) throws EFapsException
    {
        final Return ret = new Return();
        final Instance instance = _parameter.getInstance();
        if (instance != null) {
            if (instance.getType().getAttribute("Original") != null) {
                ret.put(ReturnValues.TRUE, true);
            }
        }
        return ret;
    }


    public Return create(final Parameter _parameter) throws EFapsException
    {
        final Insert insert = new Insert("Products_ImageOriginal");
        insert.add("Description", _parameter.getParameterValue("description4Create"));
        insert.execute();
        final Instance instance = insert.getInstance();
        fileUpload(_parameter, instance);

        final Instance parent = _parameter.getInstance();
        final Insert middle = new Insert("Products_Product2ImageOriginal");
        middle.add("Product", ((Long) parent.getId()).toString());
        middle.add("Image", ((Long) instance.getId()).toString());
        middle.add("Caption", _parameter.getParameterValue("caption"));
        middle.execute();

        return new Return();
    }

    /**
     * Method to upload the file.
     *
     * @param _parameter Parameter as passed from the efaps API.
     * @param _instance Instance of the new object
     * @throws EFapsException on error
     */
    protected void fileUpload(final Parameter _parameter, final Instance _instance) throws EFapsException
    {
        final Context context = Context.getThreadContext();

        final AbstractCommand command = (AbstractCommand) _parameter.get(ParameterValues.UIOBJECT);

        for (final Field field : command.getTargetForm().getFields()) {
            final String attrName = field.getExpression() == null ? field.getAttribute() : field.getExpression();
            if (attrName == null && field.isEditableDisplay(TargetMode.CREATE)) {
                final Context.FileParameter fileItem = context.getFileParameters().get(field.getName());
                if (fileItem != null) {
                    final Checkin checkin = new Checkin(_instance);
                    try {
                        checkin.execute(fileItem.getName(), fileItem.getInputStream(), (int) fileItem.getSize());

                        final ImageInfo info = Sanselan.getImageInfo(fileItem.getInputStream(), fileItem.getName());

                        final Update update = new Update(_instance);
                        update.add("WidthPx", info.getWidth());
                        update.add("HeightPx", info.getHeight());
                        update.add("HeightDPI", info.getPhysicalHeightDpi());
                        update.add("WidthDPI", info.getPhysicalWidthDpi());
                        update.add("HeightInch", info.getPhysicalHeightInch());
                        update.add("WidthInch", info.getPhysicalWidthInch());
                        update.add("NumberOfImages", info.getNumberOfImages());
                        update.add("Format", info.getFormatName());
                        update.add("ColorType", info.getColorTypeDescription());
                        update.execute();
                        final File temp = File.createTempFile("asda", "." + info.getFormat().extension);
                        final FileOutputStream os = new FileOutputStream(temp);
                        final DimensionConstrain dim = DimensionConstrain.createMaxDimension(200, 200, true);
                        if (info.getFormat().equals(ImageFormat.IMAGE_FORMAT_JPEG)) {
                            final BufferedImage img = ImageIO.read(fileItem.getInputStream());
                            final ResampleOp  resampleOp = new ResampleOp(dim);
                            final BufferedImage rescaledTomato = resampleOp.filter(img, null);
                            //final BufferedImage img2 = createThumbnail(img, 400);
                            ImageIO.write(rescaledTomato, "jpg", os);
                        } else {
                            final BufferedImage image = Sanselan.getBufferedImage(fileItem.getInputStream());
                            final ResampleOp  resampleOp = new ResampleOp(dim);
                            //resampleOp.setUnsharpenMask(AdvancedResizeOp.UnsharpenMask.Normal);
                            final BufferedImage rescaledTomato = resampleOp.filter(image, null);
                            Sanselan.writeImage(rescaledTomato, os, info.getFormat(), new HashMap());
                        }
                        os.close();

                        addImage(_parameter, _instance, temp);

                    } catch (final IOException e) {
                        throw new EFapsException(this.getClass(), "execute", e, _parameter);
                    } catch (final ImageReadException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (final ImageWriteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void addImage(final Parameter _parameter, final Instance _parent, final File _file) throws EFapsException, ImageReadException, IOException {
        final Insert insert = new Insert("Products_ImageCopy");
        insert.add("Description", _parameter.getParameterValue("description4Create"));
        insert.add("Original", _parent.getId());
        insert.execute();
        final Instance instance = insert.getInstance();
        final FileInputStream in = new FileInputStream(_file);
        final Checkin checkin = new Checkin(instance);
        checkin.execute(_file.getName(), in, ((Long) _file.length()).intValue());

        final Instance parent = _parameter.getInstance();
        final Insert middle = new Insert("Products_Product2ImageDocument");
        middle.add("Product", ((Long) parent.getId()).toString());
        middle.add("Image", ((Long) instance.getId()).toString());
        middle.add("Caption", _parameter.getParameterValue("caption"));
        middle.execute();


        final ImageInfo info = Sanselan.getImageInfo(_file);

        final Update update = new Update(instance);
        update.add("WidthPx", info.getWidth());
        update.add("HeightPx", info.getHeight());
        update.add("HeightDPI", info.getPhysicalHeightDpi());
        update.add("WidthDPI", info.getPhysicalWidthDpi());
        update.add("HeightInch", info.getPhysicalHeightInch());
        update.add("WidthInch", info.getPhysicalWidthInch());
        update.add("NumberOfImages", info.getNumberOfImages());
        update.add("Format", info.getFormatName());
        update.add("ColorType", info.getColorTypeDescription());
        update.execute();

    }


    public BufferedImage createThumbnail(final BufferedImage image, final int newSize)
    {
        int width = image.getWidth();
        int height = image.getHeight();

        final boolean isTranslucent = image.getTransparency() != Transparency.OPAQUE;
        final boolean isWidthGreater = width > height;

        if (isWidthGreater) {
            if (newSize >= width) {
                throw new IllegalArgumentException("newSize must be lower than" + " the image width");
            }
        } else if (newSize >= height) {
            throw new IllegalArgumentException("newSize must be lower than" + " the image height");
        }

        if (newSize <= 0) {
            throw new IllegalArgumentException("newSize must" + " be greater than 0");
        }

        final float ratioWH = (float) width / (float) height;
        final float ratioHW = (float) height / (float) width;

        BufferedImage thumb = image;
        BufferedImage temp = null;

        Graphics2D g2 = null;

        try {
            int previousWidth = width;
            int previousHeight = height;

            do {
                if (isWidthGreater) {
                    width /= 2;
                    if (width < newSize) {
                        width = newSize;
                    }
                    height = (int) (width / ratioWH);
                } else {
                    height /= 2;
                    if (height < newSize) {
                        height = newSize;
                    }
                    width = (int) (height / ratioHW);
                }

                if (temp == null || isTranslucent) {
                    if (g2 != null) {
                        // do not need to wrap with finally
                        // outer finally block will ensure
                        // that resources are properly reclaimed
                        g2.dispose();
                    }
                    temp = new BufferedImage(width, height, image.getType());
                    g2 = temp.createGraphics();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                }
                g2.drawImage(thumb, 0, 0, width, height, 0, 0, previousWidth, previousHeight, null);

                previousWidth = width;
                previousHeight = height;

                thumb = temp;
            } while (newSize != (isWidthGreater ? width : height));
        } finally {
            g2.dispose();
        }

        if (width != thumb.getWidth() || height != thumb.getHeight()) {
            temp = new BufferedImage(width, height, image.getType());
            g2 = temp.createGraphics();

            try {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(thumb, 0, 0, width, height, 0, 0, width, height, null);
            } finally {
                g2.dispose();
            }

            thumb = temp;
        }

        return thumb;
    }
}
