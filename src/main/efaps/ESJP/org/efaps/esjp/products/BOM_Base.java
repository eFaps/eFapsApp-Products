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
package org.efaps.esjp.products;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Attribute;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.CachedMultiPrintQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.uiform.Field;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("82271caf-5d4e-4ec9-bb99-8cbfaff5f8a7")
@EFapsApplication("eFapsApp-Products")
public abstract class BOM_Base
{

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return insertTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> values = (HashMap<?, ?>) _parameter.get(ParameterValues.NEW_VALUES);
        final Attribute attr = _parameter.getInstance().getType().getAttribute("To");

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductAbstract);
        queryBldr.addWhereAttrEqValue(CIProducts.ProductAbstract.ID, (Object[]) values.get(attr));
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.ProductAbstract.Dimension);
        Dimension dimension = null;
        if (multi.execute()) {
            multi.next();
            dimension = Dimension.get(multi.<Long>getAttribute(CIProducts.ProductAbstract.Dimension));
        }
        if (dimension != null) {
            final Update update = new Update(_parameter.getInstance());
            update.add(CIProducts.BOMAbstract.UoM, dimension.getBaseUoM().getId());
            update.execute();
        }
        return new Return();
    }

    public Return getGroupOptionListFieldValue(Parameter parameter)
        throws EFapsException
    {
        return new Field()
        {

            @Override
            protected void add2QueryBuilder4List(Parameter parameter,
                                                 QueryBuilder queryBldr)
                throws EFapsException
            {
                final var eval = EQL.builder()
                                .print(parameter.getInstance() == null ? parameter.getCallInstance()
                                                : parameter.getInstance())
                                .linkto(CIProducts.BOMAbstract.FromAbstract)
                                .instance().as("prodInst")
                                .evaluate();
                final Instance prodInst = eval.get("prodInst");
                queryBldr.addWhereAttrEqValue(CIProducts.BOMGroupAbstract.ProductAbstractLink, prodInst);
            }
        }.getOptionListFieldValue(parameter);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return editBOMQuantity(final Parameter _parameter)
        throws EFapsException
    {
        @SuppressWarnings("unchecked") final Map<String, String> oidMap = (Map<String, String>) _parameter
                        .get(ParameterValues.OIDMAP4UI);
        final String[] rowKeys = _parameter.getParameterValues(EFapsKey.TABLEROW_NAME.getKey());
        final String[] quantity = _parameter.getParameterValues("quantity");
        for (int i = 0; i < rowKeys.length; i++) {
            final Instance inst = Instance.get(oidMap.get(rowKeys[i]));
            final Update update = new Update(inst);
            update.add(CIProducts.BOMAbstract.Quantity, quantity[i]);
            update.execute();
        }
        return new Return();
    }

    public List<ProductBOMBean> getBOMProducts(final Parameter _parameter,
                                               final Instance _prodInst,
                                               final BigDecimal _quantity,
                                               final UoM _uoM)
        throws EFapsException
    {
        BigDecimal quantity = _quantity;
        // if an uom is given ensure that is normed on base
        if (_uoM != null && !_uoM.equals(_uoM.getDimension().getBaseUoM())) {
            quantity = quantity.multiply(new BigDecimal(_uoM.getDenominator()))
                            .setScale(8, RoundingMode.HALF_UP)
                            .divide(new BigDecimal(_uoM.getNumerator()), RoundingMode.HALF_UP);

        }
        return getBOMProducts(_parameter, _prodInst, quantity);
    }

    public List<ProductBOMBean> getBOMProducts(final Parameter _parameter,
                                               final Instance _prodInst,
                                               final BigDecimal _quantity)
        throws EFapsException
    {
        final List<ProductBOMBean> ret = new ArrayList<>();

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductionBOM);
        queryBldr.addWhereAttrEqValue(CIProducts.ProductionBOM.From, _prodInst);
        final CachedMultiPrintQuery multi = queryBldr.getCachedPrint4Request();
        final SelectBuilder selBOMProd = SelectBuilder.get().linkto(CIProducts.ProductionBOM.To);
        final SelectBuilder selBOMProdInst = new SelectBuilder(selBOMProd).instance();
        final SelectBuilder selBOMProdName = new SelectBuilder(selBOMProd).attribute(CIProducts.ProductAbstract.Name);
        final SelectBuilder selBOMProdDescr = new SelectBuilder(selBOMProd)
                        .attribute(CIProducts.ProductAbstract.Description);
        final SelectBuilder selBOMProdUoM = new SelectBuilder(selBOMProd)
                        .attribute(CIProducts.ProductAbstract.DefaultUoM);
        multi.addSelect(selBOMProdInst, selBOMProdName, selBOMProdDescr, selBOMProdUoM);
        multi.addAttribute(CIProducts.BOMAbstract.Quantity, CIProducts.BOMAbstract.UoM);
        multi.execute();
        while (multi.next()) {
            final ProductBOMBean bean = getProductBOMBean(_parameter);
            ret.add(bean);
            bean.setInstance(multi.<Instance>getSelect(selBOMProdInst))
                            .setName(multi.<String>getSelect(selBOMProdName))
                            .setDescription(multi.<String>getSelect(selBOMProdDescr));

            final Long defaultUoMID = multi.<Long>getSelect(selBOMProdUoM);
            final UoM uom = Dimension.getUoM(multi.<Long>getAttribute(CIProducts.BOMAbstract.UoM));
            BigDecimal quantity = multi.getAttribute(CIProducts.BOMAbstract.Quantity);

            if (defaultUoMID != null) {
                final UoM defaultUoM = Dimension.getUoM(defaultUoMID);
                if (!defaultUoM.equals(uom)) {
                    quantity = quantity.multiply(new BigDecimal(uom.getNumerator()))
                                    .setScale(8, BigDecimal.ROUND_HALF_UP)
                                    .divide(new BigDecimal(uom.getDenominator()), BigDecimal.ROUND_HALF_UP);
                    quantity = quantity.multiply(new BigDecimal(defaultUoM.getDenominator()))
                                    .setScale(8, BigDecimal.ROUND_HALF_UP)
                                    .divide(new BigDecimal(defaultUoM.getNumerator()), BigDecimal.ROUND_HALF_UP);
                }
                bean.setUoM(defaultUoM);
            } else if (!uom.equals(uom.getDimension().getBaseUoM())) {
                quantity = quantity.multiply(new BigDecimal(uom.getNumerator()))
                                .setScale(8, BigDecimal.ROUND_HALF_UP)
                                .divide(new BigDecimal(uom.getDenominator()), BigDecimal.ROUND_HALF_UP);
                bean.setUoM(uom.getDimension().getBaseUoM());
            } else {
                bean.setUoM(uom);
            }
            bean.setQuantity(quantity.multiply(_quantity));
        }
        return ret;
    }

    protected ProductBOMBean getProductBOMBean(final Parameter _parameter)
    {
        return new ProductBOMBean();
    }



    public static class ProductBOMBean
    {

        private Instance instance;

        private String name;

        private String description;

        private UoM uoM;

        private BigDecimal quantity;

        /**
         * Getter method for the instance variable {@link #instance}.
         *
         * @return value of instance variable {@link #instance}
         */
        public Instance getInstance()
        {
            return this.instance;
        }

        /**
         * Setter method for instance variable {@link #instance}.
         *
         * @param _instance value for instance variable {@link #instance}
         */
        public ProductBOMBean setInstance(final Instance _instance)
        {
            this.instance = _instance;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #name}.
         *
         * @return value of instance variable {@link #name}
         */
        public String getName()
        {
            return this.name;
        }

        /**
         * Setter method for instance variable {@link #name}.
         *
         * @param _name value for instance variable {@link #name}
         */
        public ProductBOMBean setName(final String _name)
        {
            this.name = _name;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #description}.
         *
         * @return value of instance variable {@link #description}
         */
        public String getDescription()
        {
            return this.description;
        }

        /**
         * Setter method for instance variable {@link #description}.
         *
         * @param _description value for instance variable {@link #description}
         */
        public ProductBOMBean setDescription(final String _description)
        {
            this.description = _description;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #uoM}.
         *
         * @return value of instance variable {@link #uoM}
         */
        public UoM getUoM()
        {
            return this.uoM;
        }

        /**
         * Setter method for instance variable {@link #uoM}.
         *
         * @param _uoM value for instance variable {@link #uoM}
         */
        public ProductBOMBean setUoM(final UoM _uoM)
        {
            this.uoM = _uoM;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #quantity}.
         *
         * @return value of instance variable {@link #quantity}
         */
        public BigDecimal getQuantity()
        {
            return this.quantity;
        }

        /**
         * Setter method for instance variable {@link #quantity}.
         *
         * @param _quantity value for instance variable {@link #quantity}
         */
        public ProductBOMBean setQuantity(final BigDecimal _quantity)
        {
            this.quantity = _quantity;
            return this;
        }

    }
}
