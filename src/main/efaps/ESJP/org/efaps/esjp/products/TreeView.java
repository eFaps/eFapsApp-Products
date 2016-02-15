/*
 * Copyright 2003 - 2016 The eFaps Team
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
 */

package org.efaps.esjp.products;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.util.EFapsException;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_base</code>"
 * class.
 *
 * @author The eFaps Team
 */
@EFapsUUID("f70f4fc0-6cf5-42e2-92d9-081b2ddc17e2")
@EFapsApplication("eFapsApp-Products")
public class TreeView
    extends TreeView_Base
{

    /** The Constant CACHEKEY. */
    public static final String CACHEKEY = TreeView_Base.CACHEKEY;

    /**
     * Gets the tree view label.
     *
     * @param _parameter the _parameter
     * @param _treeViewInst the _tree view inst
     * @param _productInst the _product inst
     * @return the tree view label
     * @throws EFapsException the e faps exception
     */
    public static String getTreeViewLabel(final Parameter _parameter,
                                          final Instance _treeViewInst,
                                          final Instance _productInst)
        throws EFapsException
    {
        return TreeView_Base.getTreeViewLabel(_parameter, _treeViewInst, _productInst);
    }


    /**
     * Gets the tree view label.
     *
     * @param _parameter the _parameter
     * @param _treeViewInst the _tree view inst
     * @param _productInst the _product inst
     * @param _hideRoot the _hide root
     * @param _hideProduct the _hide product
     * @return the tree view label
     * @throws EFapsException the e faps exception
     */
    public static String getTreeViewLabel(final Parameter _parameter,
                                          final Instance _treeViewInst,
                                          final Instance _productInst,
                                          final boolean _hideRoot,
                                          final boolean _hideProduct)
        throws EFapsException
    {
        return TreeView_Base.getTreeViewLabel(_parameter, _treeViewInst, _productInst, _hideRoot, _hideProduct);
    }
}
