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

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.EventExecution;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.ui.wicket.models.objects.UIStructurBrowser;
import org.efaps.ui.wicket.models.objects.UIStructurBrowser.ExecutionStatus;
import org.efaps.util.EFapsException;

/**
 * TODO description!
 *
 * @author The eFasp Team
 * @version $Id$
 */
@EFapsUUID("c7f05800-d551-487c-9ed6-c34f31e4f7c8")
@EFapsRevision("$Rev$")
public abstract class TreeViewStructurBrowser_Base
    implements EventExecution
{

    /**
     * @param _parameter Parameter
     * @throws EFapsException on error
     * @return Return
     */
    public Return execute(final Parameter _parameter) throws EFapsException
    {
        Return ret = null;

        final UIStructurBrowser strBro = (UIStructurBrowser) _parameter.get(ParameterValues.CLASS);
        final ExecutionStatus status = strBro.getExecutionStatus();
        if (status.equals(ExecutionStatus.EXECUTE)) {
            ret = internalExecute(_parameter);
        } else if (status.equals(ExecutionStatus.CHECKFORCHILDREN)) {
            ret = checkForChildren(_parameter.getInstance());
        } else if (status.equals(ExecutionStatus.ADDCHILDREN)) {
            ret = addChildren(_parameter.getInstance());
        } else if (status.equals(ExecutionStatus.SORT)) {
            ret = sort(strBro);
        }
        return ret;
    }

    /**
     * Method to get a list of instances the StructurBrowser will be filled
     * with.
     * @param _parameter as passed from eFaps API.
     * @return Return with instances
     * @throws EFapsException on error
     */
    private Return internalExecute(final Parameter _parameter) throws EFapsException
    {
        final Return ret = new Return();
        final Map<Instance, Boolean> tree = new LinkedHashMap<Instance, Boolean>();
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String typesStr = (String) properties.get("Types");
        final String[] types = typesStr.split(";");
        for (final String type : types) {
            final QueryBuilder queryBldr = new QueryBuilder(Type.get(type));
            final InstanceQuery query = queryBldr.getQuery();
            query.execute();
            while (query.next()) {
                tree.put(query.getCurrentValue(), null);
            }
        }
        ret.put(ReturnValues.VALUES, tree);
        return ret;
    }

    /**
     * Method to check if an instance has children. It is used in the tree to
     * determine if a "plus" to open the children must be rendered.
     *
     * @param _instance Instance to check for children
     * @return Return with true or false
     * @throws EFapsException on error
     */
    private Return checkForChildren(final Instance _instance)
        throws EFapsException
    {
        final Return ret = new Return();
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TreeViewNode);
        queryBldr.addWhereAttrEqValue(CIProducts.TreeViewNode.ParentLink, _instance.getId());
        final InstanceQuery query = queryBldr.getQuery();
        query.setLimit(1);
        query.execute();
        if (query.next()) {
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    /**
     * Method to add the children to an instance. It is used to expand the
     * children of a node in the tree.
     *
     * @param _instance Instance the children must be retrieved for.
     * @return Return with instances
     * @throws EFapsException on error
     */
    private Return addChildren(final Instance _instance)
        throws EFapsException
    {
        final Return ret = new Return();
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TreeViewNode);
        queryBldr.addWhereAttrEqValue(CIProducts.TreeViewNode.ParentLink, _instance.getId());
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        final Map<Instance, Boolean> map = new LinkedHashMap<Instance, Boolean>();
        while (query.next()) {
            map.put(query.getCurrentValue(), null);
        }
        ret.put(ReturnValues.VALUES, map);
        return ret;
    }

    /**
     * Method to sort the values of the StructurBrowser.
     *
     * @param _structurBrowser _sructurBrowser to be sorted
     * @return empty Return;
     */
    private Return sort(final UIStructurBrowser _structurBrowser)
    {
        Collections.sort(_structurBrowser.getChilds(), new Comparator<UIStructurBrowser>() {

            public int compare(final UIStructurBrowser _structurBrowser1, final UIStructurBrowser _structurBrowser2)
            {

                final String value1 = getSortString(_structurBrowser1);
                final String value2 = getSortString(_structurBrowser2);

                return value1.compareTo(value2);
            }

            private String getSortString(final UIStructurBrowser _structurBrowser)
            {
                final StringBuilder ret = new StringBuilder();
                try {
                    if (_structurBrowser.getInstance() != null) {
                        _structurBrowser.getInstance().getType();
                    }
                } catch (final EFapsException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                ret.append(_structurBrowser.getLabel());
                return ret.toString();
            }
        });
        return new Return();
    }

}
