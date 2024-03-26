/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.products.reports.filter;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.uiform.Field;
import org.efaps.esjp.common.uiform.Field_Base.DropDownPosition;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.FilteredReport_Base.AbstractFilterValue;
import org.efaps.util.EFapsException;

/**
 * The Class CostTypeFilterValue_Base.
 *
 * @author The eFaps Team
 */
@EFapsUUID("fd3e8054-4c20-4a9d-aa7f-32fe7555040d")
@EFapsApplication("eFapsApp-Products")
public abstract class CostTypeFilterValue_Base
    extends AbstractFilterValue<String>
{
    /** The Constant DEFAULT. */
    protected static final String DEFAULT = "STANDART";

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /** The alternative. */
    private boolean alternative;

    /**
     * Getter method for the instance variable {@link #alternative}.
     *
     * @return value of instance variable {@link #alternative}
     */
    public boolean isAlternative()
    {
        return this.alternative;
    }

    /**
     * Setter method for instance variable {@link #alternative}.
     *
     * @param _alternative value for instance variable {@link #alternative}
     * @return the cost type filter value
     */
    public CostTypeFilterValue setAlternative(final boolean _alternative)
    {
        this.alternative = _alternative;
        return (CostTypeFilterValue) this;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return the label for this filter
     * @throws EFapsException on error
     */
    @Override
    public String getLabel(final Parameter _parameter)
        throws EFapsException
    {
        final String ret;
        final Instance tmpInst = Instance.get(getObject());
        if (getObject() == null) {
            ret = DBProperties.getProperty(CostTypeFilterValue.class.getName() + ".CostType."
                            + CostTypeFilterValue.DEFAULT);
        } else if (isAlternative() && InstanceUtils.isKindOf(tmpInst, CIERP.Currency)) {
            ret = DBProperties.getFormatedDBProperty(CostTypeFilterValue.class.getName()
                            + ".CostType.Alternative",
                            (Object) CurrencyInst.get(Instance.get(getObject())).getName());
        } else {
            ret = DBProperties.getProperty(CostTypeFilterValue.class.getName() + ".CostType." + getObject());
        }
        return ret;
    }

    @Override
    public AbstractFilterValue<String> parseObject(final String[] _values)
    {
        if (!ArrayUtils.isEmpty(_values) && _values[0].startsWith("ALTERNATIVE_")) {
            setAlternative(true);
            setObject(_values[0].replace("ALTERNATIVE_", ""));
        } else {
            setAlternative(false);
        }
        if (!ArrayUtils.isEmpty(_values) && _values[0].equals(CostTypeFilterValue.DEFAULT)) {
            setObject(CostTypeFilterValue.DEFAULT);
        }
        return super.parseObject(_values);
    }

    /**
     * Gets the cost type field value.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _selected the selected
     * @return the cost type field value
     * @throws EFapsException on error
     */
    protected static List<DropDownPosition> getCostTypePositions(final Parameter _parameter,
                                                                 final CostTypeFilterValue _selected)
        throws EFapsException
    {
        final List<DropDownPosition> ret = new ArrayList<>();
        ret.add(new Field.DropDownPosition(CostTypeFilterValue.DEFAULT, new CostTypeFilterValue().getLabel(_parameter))
                        .setSelected(_selected == null || _selected.getObject() == null
                        || CostTypeFilterValue.DEFAULT.equals(_selected.getObject())));
        for (final CurrencyInst currencyInst : CurrencyInst.getAvailable()) {
            if (!currencyInst.getInstance().equals(Currency.getBaseCurrency())) {
                final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductCostAlternative);
                queryBldr.addWhereAttrEqValue(CIProducts.ProductCostAlternative.CurrencyLink,
                                currencyInst.getInstance());
                queryBldr.setLimit(1);
                if (!queryBldr.getQuery().executeWithoutAccessCheck().isEmpty()) {
                    final boolean selected = _selected != null && _selected.isAlternative()
                                    && currencyInst.getInstance().getOid().equals(_selected.getObject());
                    ret.add(new Field.DropDownPosition("ALTERNATIVE_" + currencyInst.getInstance().getOid(),
                                        new CostTypeFilterValue()
                                            .setAlternative(true)
                                            .setObject(currencyInst.getInstance().getOid()).getLabel(_parameter))
                                    .setSelected(selected));
                }
            }
        }
        return ret;
    }
}


