/*
 * Copyright 2003 - 2010 The eFaps Team
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Instance;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ui.structurbrowser.StandartStructurBrowser;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO description!
 *
 * @author The eFasp Team
 * @version $Id$
 */
@EFapsUUID("c7f05800-d551-487c-9ed6-c34f31e4f7c8")
@EFapsRevision("$Rev$")
public abstract class TreeViewStructurBrowser_Base
    extends StandartStructurBrowser
{
    /**
     * Logger for this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(TreeViewStructurBrowser_Base.class);

    /**
     * Method to check if an instance allows children. It is used in the tree to
     * determine "folder" or an "item" must be rendered and if the checkForChildren
     * method must be executed.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return Return with true or false
     */
    @Override
    protected Return allowChildren(final Parameter _parameter)
    {
        final Return ret = new Return();
        final Instance inst = _parameter.getInstance();
        if (inst != null && inst.isValid()) {
            if (!inst.getType().isKindOf(CIProducts.TreeViewProduct.getType())) {
                ret.put(ReturnValues.TRUE, true);
            }
        }
        return ret;
    }

    @Override
    protected void addCriteria4Children(final Parameter _parameter,
                                        final QueryBuilder _queryBldr)
        throws EFapsException
    {
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String productTypesStr = (String) properties.get("ProductTypes");

        final Type type = Type.get(_queryBldr.getTypeUUID());
        if (type.isKindOf(CIProducts.TreeViewProduct.getType())
                        && productTypesStr != null && !productTypesStr.isEmpty()) {
            final String[] productTypes = productTypesStr.split(";");
            final List<Long> typeIds = new ArrayList<Long>();
            for (final String prodTypeStr : productTypes) {
                typeIds.add(Type.get(prodTypeStr).getId());
            }
            final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.ProductAbstract);
            attrQueryBldr.addWhereAttrEqValue(CIProducts.ProductAbstract.Type, typeIds.toArray());
            final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CIProducts.ProductAbstract.ID);

            _queryBldr.addWhereAttrInQuery(CIProducts.TreeViewProduct.ProductLink, attrQuery);
        }
    }
}
