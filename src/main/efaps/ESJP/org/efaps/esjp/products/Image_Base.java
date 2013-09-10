/*
 * Copyright 2003 - 2011 The eFaps Team
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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.sanselan.ImageFormat;
import org.apache.sanselan.ImageInfo;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.Sanselan;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractCommand;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.admin.ui.field.Field;
import org.efaps.db.Checkin;
import org.efaps.db.Context;
import org.efaps.db.Context.FileParameter;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.file.FileUtil;
import org.efaps.esjp.common.file.ImageField;
import org.efaps.esjp.products.util.Products;
import org.efaps.esjp.products.util.ProductsSettings;
import org.efaps.util.EFapsException;

import com.mortennobel.imagescaling.DimensionConstrain;
import com.mortennobel.imagescaling.ResampleOp;

/**
 * TODO comment! First fast draft for testing purpose only!!!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("ac49af4b-e10e-43f8-976d-8b5170dbfb7a")
@EFapsRevision("$Rev$")
public abstract class Image_Base
{

    /**
     * Access is defined by a SystemConfiguration.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return with true if access is granted
     * @throws EFapsException on error
     */
    public Return access4Image(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final SystemConfiguration config = Products.getSysConfig();
        if (config != null) {
            if (config.getAttributeValueAsBoolean(ProductsSettings.ACTIVATEIMAGE)) {
                ret.put(ReturnValues.TRUE, true);
            }
        }
        return ret;
    }

    /**
     * Grants access to the field used as a link to the original file.
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return with true if access is granted
     * @throws EFapsException on error
     */
    public Return access4OriginalFieldUI(final Parameter _parameter)
        throws EFapsException
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

    /**
     * Grants access to the field used as a link to the original file.
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return with true if access is granted
     * @throws EFapsException on error
     */
    public Return getImageFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final Type type;
        if (properties.containsKey("RelationType")) {
            type = Type.get((String) properties.get("RelationType"));
        } else {
            type = CIProducts.Product2ImageAbstract.getType();
        }
        final QueryBuilder queryBldr = new QueryBuilder(type);
        queryBldr.addWhereAttrEqValue(CIProducts.Product2ImageAbstract.ProductAbstractLink,
                        _parameter.getInstance().getId());
        final InstanceQuery query = queryBldr.getQuery();
        query.setLimit(1);
        final List<Instance> res = query.execute();
        final String imageOid;
        final String linkOid;
        if (!res.isEmpty()) {
            final PrintQuery print = new PrintQuery(res.get(0));
            final SelectBuilder selImageOid = new SelectBuilder()
                .linkto(CIProducts.Product2ImageAbstract.ImageAbstractLink).oid();
            print.addSelect(selImageOid);
            print.execute();
            imageOid = print.<String>getSelect(selImageOid);
            final Instance imageInst = Instance.get(imageOid);
            if (imageInst.getType().equals(CIProducts.ImageThumbnail.getType())) {
                final PrintQuery imPrint = new PrintQuery(imageInst);
                final SelectBuilder selLinkOid = new SelectBuilder()
                    .linkto(CIProducts.ImageThumbnail.OriginalLink).oid();
                imPrint.addSelect(selLinkOid);
                imPrint.execute();
                linkOid = imPrint.<String>getSelect(selLinkOid);
            } else {
                linkOid = imageOid;
            }
        } else {
            linkOid = null;
            imageOid = null;
        }
        Return retVal;
        if (imageOid != null) {
            retVal = new ImageField(){

                @Override
                protected String getImageOid(final Parameter _parameter)
                    throws EFapsException
                {
                    return imageOid ;
                }

                @Override
                protected String getTargetOid(final Parameter _parameter)
                    throws EFapsException
                {
                    return linkOid ;
                }

            }.getViewFieldValueUI(_parameter);
        } else {
            retVal = new Return();
            retVal.put(ReturnValues.SNIPLETT, "");
        }
        return retVal;
    }


    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return with true if access is granted
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Instance parentInst = _parameter.getInstance();

        final Instance imageInst = createImage(_parameter, CIProducts.ImageOriginal.getType(), null);
        connectImage(_parameter, CIProducts.Product2ImageOriginal.getType(), parentInst, imageInst);
        final FileParameter fileItem = getFileParameter(_parameter);
        uploadImage(_parameter, imageInst, fileItem);
        final Properties props = getProperties(_parameter);
        if (props.containsKey("Image4Doc_Create") && "true".equalsIgnoreCase(props.getProperty("Image4Doc_Create"))) {
            final int width = Integer.parseInt(props.getProperty("Image4Doc_Width", "250"));
            final int height = Integer.parseInt(props.getProperty("Image4Doc_Height", "250"));
            final boolean enlarge = "true".equalsIgnoreCase(props.getProperty("Image4Doc_Enlarge", "false"));
            final DimensionConstrain dim = DimensionConstrain.createMaxDimension(width, height, !enlarge);
            final File img = createNewImageFile(_parameter, "Image4Doc_", fileItem, dim);
            final Instance copyInst = createImage(_parameter, CIProducts.ImageDocument.getType(), imageInst);
            connectImage(_parameter, CIProducts.Product2ImageDocument.getType(), parentInst, copyInst);
            uploadImage(_parameter, copyInst, img);
        }
        if (props.containsKey("Thumbnail_Create") && "true".equalsIgnoreCase(props.getProperty("Thumbnail_Create"))) {
            final int width = Integer.parseInt(props.getProperty("Thumbnail_Width", "150"));
            final int height = Integer.parseInt(props.getProperty("Thumbnail_Height", "150"));
            final boolean enlarge = "true".equalsIgnoreCase(props.getProperty("Thumbnail_Enlarge", "false"));
            final DimensionConstrain dim = DimensionConstrain.createMaxDimension(width, height, !enlarge);
            final File img = createNewImageFile(_parameter, "Thumbnail_", fileItem, dim);
            final Instance copyInst = createImage(_parameter, CIProducts.ImageThumbnail.getType(), imageInst);
            connectImage(_parameter, CIProducts.Product2ImageThumbnail.getType(), parentInst, copyInst);
            uploadImage(_parameter, copyInst, img);
        }
        return new Return();
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _prefix       Prefix
     * @param _fileItem     FileItem the new image will be created from
     * @param _dim          Dimension of the new image
     * @throws EFapsException on error
     * @return new File
     */
    @SuppressWarnings("rawtypes")
    protected File createNewImageFile(final Parameter _parameter,
                                      final String _prefix,
                                      final FileParameter _fileItem,
                                      final DimensionConstrain _dim)
        throws EFapsException
    {
        final File ret;
        try {
            final ImageInfo info = Sanselan.getImageInfo(_fileItem.getInputStream(), _fileItem.getName());
            ret = new FileUtil().getFile(_prefix + _fileItem.getName(), info.getFormat().extension);

            final FileOutputStream os = new FileOutputStream(ret);

            if (info.getFormat().equals(ImageFormat.IMAGE_FORMAT_JPEG)) {
                final BufferedImage img = ImageIO.read(_fileItem.getInputStream());
                final ResampleOp resampleOp = new ResampleOp(_dim);
                final BufferedImage rescaledTomato = resampleOp.filter(img, null);
                ImageIO.write(rescaledTomato, "jpg", os);
            } else {
                final BufferedImage image = Sanselan.getBufferedImage(_fileItem.getInputStream());
                final ResampleOp resampleOp = new ResampleOp(_dim);
                // resampleOp.setUnsharpenMask(AdvancedResizeOp.UnsharpenMask.Normal);
                final BufferedImage rescaledTomato = resampleOp.filter(image, null);
                Sanselan.writeImage(rescaledTomato, os, info.getFormat(), new HashMap());
            }
        } catch (final ImageReadException e) {
            throw new EFapsException(this.getClass(), "createNewImage", e, _parameter);
        } catch (final IOException e) {
            throw new EFapsException(this.getClass(), "createNewImage", e, _parameter);
        } catch (final ImageWriteException e) {
            throw new EFapsException(this.getClass(), "createNewImage", e, _parameter);
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return FileParameter from the context
     * @throws EFapsException on error
     */
    protected FileParameter getFileParameter(final Parameter _parameter)
        throws EFapsException
    {
        final Context context = Context.getThreadContext();

        final AbstractCommand command = (AbstractCommand) _parameter.get(ParameterValues.UIOBJECT);
        FileParameter ret = null;
        for (final Field field : command.getTargetForm().getFields()) {
            final String attrName = field.getAttribute();
            if (attrName == null && field.isEditableDisplay(TargetMode.CREATE)) {
                final FileParameter fileItem = context.getFileParameters().get(field.getName());
                if (fileItem != null) {
                    ret = fileItem;
                    break;
                }
            }
        }
        return ret;
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _imageInst    Instance the imgae will be check in to
     * @param _fileItem      file to be checked in
     * @throws EFapsException on error
     */
    protected void uploadImage(final Parameter _parameter,
                               final Instance _imageInst,
                               final FileParameter _fileItem)
        throws EFapsException
    {
        final Checkin checkin = new Checkin(_imageInst);
        try {
            checkin.execute(_fileItem.getName(), _fileItem.getInputStream(), (int) _fileItem.getSize());
            final ImageInfo info = Sanselan.getImageInfo(_fileItem.getInputStream(), _fileItem.getName());
            updateImage(_parameter, _imageInst, info);
        } catch (final IOException e) {
            throw new EFapsException(this.getClass(), "uploadImage", e, _parameter);
        } catch (final ImageReadException e) {
            throw new EFapsException(this.getClass(), "uploadImage", e, _parameter);
        }
    }


    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _imageInst    Instance the imgae will be check in to
     * @param _file         file to be checked in
     * @throws EFapsException on error
     */
    protected void uploadImage(final Parameter _parameter,
                               final Instance _imageInst,
                               final File _file)
        throws EFapsException
    {
        final Checkin checkin = new Checkin(_imageInst);
        try {
            final FileInputStream in = new FileInputStream(_file);
            checkin.execute(_file.getName(), in, ((Long) _file.length()).intValue());
            final ImageInfo info = Sanselan.getImageInfo(_file);
            updateImage(_parameter, _imageInst, info);
        } catch (final IOException e) {
            throw new EFapsException(this.getClass(), "uploadImage", e, _parameter);
        } catch (final ImageReadException e) {
            throw new EFapsException(this.getClass(), "uploadImage", e, _parameter);
        }
    }


    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _imageInst    instance of the image to be update with the info
     * @param _info         ImageInfo
     * @throws EFapsException on error
     */
    protected void updateImage(final Parameter _parameter,
                               final Instance _imageInst,
                               final ImageInfo _info)
        throws EFapsException
    {
        final Update update = new Update(_imageInst);
        update.add(CIProducts.ImageAbstract.WidthPx, _info.getWidth());
        update.add(CIProducts.ImageAbstract.HeightPx, _info.getHeight());
        update.add(CIProducts.ImageAbstract.HeightDPI, _info.getPhysicalHeightDpi());
        update.add(CIProducts.ImageAbstract.WidthDPI, _info.getPhysicalWidthDpi());
        update.add(CIProducts.ImageAbstract.HeightInch, _info.getPhysicalHeightInch());
        update.add(CIProducts.ImageAbstract.WidthInch, _info.getPhysicalWidthInch());
        update.add(CIProducts.ImageAbstract.NumberOfImages, _info.getNumberOfImages());
        update.add(CIProducts.ImageAbstract.Format, _info.getFormatName());
        update.add(CIProducts.ImageAbstract.ColorType, _info.getColorTypeDescription());
        update.execute();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return properties for Images
     * @throws EFapsException on error
     */
    protected Properties getProperties(final Parameter _parameter)
        throws EFapsException
    {
        Properties props = null;
        // Products-Configuration
        final SystemConfiguration config = SystemConfiguration.get(
                        UUID.fromString("e53cd705-e463-47dc-a400-4ace4ed72071"));
        if (config != null) {
            props = config.getAttributeValueAsProperties("ImagesProperties");
        }
        return props;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _type Type of connection
     * @param _imageInst Instance of the Image to be connected
     * @param _prodInst instance of the product to connect to
     * @return instance of newly create connection
     * @throws EFapsException on error
     */
    protected Instance connectImage(final Parameter _parameter,
                                    final Type _type,
                                    final Instance _prodInst,
                                    final Instance _imageInst)
        throws EFapsException
    {
        final Insert insert = new Insert(_type);
        insert.add(CIProducts.Product2ImageAbstract.ProductAbstractLink, _prodInst.getId());
        insert.add(CIProducts.Product2ImageAbstract.ImageAbstractLink, _imageInst.getId());
        insert.add(CIProducts.Product2ImageAbstract.Caption, _parameter.getParameterValue("caption"));
        insert.execute();
        return insert.getInstance();
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _type Type    of image to be created
     * @param _origInst     Instance of the original file
     * @return instance of newly create file
     * @throws EFapsException on error
     */
    protected Instance createImage(final Parameter _parameter,
                                   final Type _type,
                                   final Instance _origInst)
        throws EFapsException
    {
        final Insert insert = new Insert(_type);
        insert.add(CIProducts.ImageAbstract.Description, _parameter.getParameterValue("description4Create"));
        if (_origInst != null && _type.isKindOf(CIProducts.ImageCopy.getType())) {
            insert.add(CIProducts.ImageCopy.OriginalAbstractLink, _origInst.getId());
        }
        insert.execute();
        return insert.getInstance();
    }
}
