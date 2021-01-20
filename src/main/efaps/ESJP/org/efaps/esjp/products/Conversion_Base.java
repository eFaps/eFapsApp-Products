/*
 * Copyright 2003 - 2020 The eFaps Team
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.ui.UIValue;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.properties.PropertiesUtil;
import org.efaps.esjp.common.uiform.Field_Base.DropDownPosition;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.products.util.ConversionType;
import org.efaps.esjp.products.util.Products;
import org.efaps.util.EFapsException;
import org.efaps.util.UUIDUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("c635be92-213a-4f5c-89fe-d5b37b11bc85")
@EFapsApplication("eFapsApp-Products")
public abstract class Conversion_Base
{
    private static final Logger LOG = LoggerFactory.getLogger(Conversion.class);

    public Return fromUoMFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final var instance = (Instance) _parameter.get(ParameterValues.CALL_INSTANCE);
        final List<DropDownPosition> values = new ArrayList<>();
        Long uomId = null;
        if (InstanceUtils.isKindOf(instance, CIProducts.ProductAbstract)) {
            final var eval = EQL.builder().print(instance).attribute(CIProducts.ProductAbstract.DefaultUoM).evaluate();
            uomId = eval.get(CIProducts.ProductAbstract.DefaultUoM);
        } else if (InstanceUtils.isType(instance, CIProducts.Conversion)) {
            final var value = (UIValue) _parameter.get(ParameterValues.UIOBJECT);
            uomId = (Long) value.getDbValue();
        }
        if (uomId != null) {
            final Dimension dim = Dimension.getUoM(uomId).getDimension();
            for (final UoM uoM : dim.getUoMs()) {
                values.add(new DropDownPosition(uoM.getId(), uoM.getName())
                                .setSelected(dim.getBaseUoM() != null && dim.getBaseUoM().getId() == uomId));
            }
        }
        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, values);
        return ret;
    }

    public Return toUoMFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        var instance = (Instance) _parameter.get(ParameterValues.CALL_INSTANCE);
        Properties props;
        long uomId = 0;
        if (InstanceUtils.isType(instance, CIProducts.Conversion)) {
            final var eval = EQL.builder().print(instance).linkto(CIProducts.Conversion.ProductLink).as("ProdInst")
                            .instance().evaluate();
            instance = eval.get("ProdInst");
            final var value = (UIValue) _parameter.get(ParameterValues.UIOBJECT);
            uomId = (Long) value.getDbValue();
        }
        if (InstanceUtils.isType(instance, CIProducts.ProductStandart)) {
            props = Products.STANDART_CONV.get();
        } else {
            props = new Properties();
        }
        final List<DropDownPosition> values = new ArrayList<>();
        if (instance != null) {
            for (final var dimKey : PropertiesUtil.analyseProperty(props, "Dimension", 0).values()) {
                final Dimension dim = UUIDUtil.isUUID(dimKey) ? Dimension.get(UUID.fromString(dimKey))
                                : Dimension.get(dimKey);
                if (dim != null) {
                    for (final UoM uoM : dim.getUoMs()) {
                        values.add(new DropDownPosition(uoM.getId(), dim.getName() + " - " + uoM.getName())
                                        .setSelected(uoM.getId() == uomId));
                    }
                }
            }
        }
        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, values);
        return ret;
    }

    protected static ConversionValue convert(final ConversionType _conversionType, final Instance _productInstance,
                                             final BigDecimal _quantity, final UoM _uoM)
        throws EFapsException
    {
        ConversionValue ret = null;
        final var eval = EQL.builder()
            .print()
            .query(CIProducts.Conversion)
            .where()
                .attribute(CIProducts.Conversion.ConversionType).eq(1)
                .and()
                .attribute(CIProducts.Conversion.ProductLink).eq(_productInstance)
            .select()
                .attribute(CIProducts.Conversion.FromQuantity)
                .attribute(CIProducts.Conversion.FromUoM)
                .attribute(CIProducts.Conversion.ToQuantity)
                .attribute(CIProducts.Conversion.ToUoM)
             .limit(1)
             .evaluate();
        if (eval.next()) {
            final Integer fromInt = eval.get(CIProducts.Conversion.FromQuantity);
            final Long fromUoMID = eval.get(CIProducts.Conversion.FromUoM);
            final Integer toInt = eval.get(CIProducts.Conversion.ToQuantity);
            final Long toUoMID = eval.get(CIProducts.Conversion.ToUoM);

            final var fromUoM = Dimension.getUoM(fromUoMID);
            final var toUoM = Dimension.getUoM(toUoMID);
            BigDecimal from = null;
            // base UoM and the used UoM are the same
            if (fromUoM.equals(_uoM)) {
                from = new BigDecimal(fromInt);
            } else {
                if (fromUoM.getDimId() == _uoM.getDimId()) {
                    from = new BigDecimal(fromInt);
                    from = from.multiply(new BigDecimal(_uoM.getNumerator())).divide(
                                    new BigDecimal(_uoM.getDenominator()), RoundingMode.HALF_UP);
                } else {
                    LOG.error("Invalid conversion definition for {}", _productInstance);
                }
            }
            if (from != null) {
                final var to = new BigDecimal(toInt);
                final var multiplier = to.divide(from, 8, RoundingMode.HALF_UP);
                var value = _quantity.multiply(multiplier);
                if (!toUoM.equals(toUoM.getDimension().getBaseUoM())) {
                    value = value.multiply(new BigDecimal(toUoM.getDenominator())).divide(
                                    new BigDecimal(toUoM.getNumerator()), 8, RoundingMode.HALF_UP);
                }

                ret = new ConversionValue(value, toUoM);
            }
        }
        return ret;
    }

    public static class ConversionValue
    {

        private final BigDecimal value;
        private final UoM uoM;

        public ConversionValue(final BigDecimal _value,
                               final UoM _uoM)
        {
            value = _value;
            uoM = _uoM;
        }

        public BigDecimal getValue()
        {
            return value;
        }

        public UoM getUoM()
        {
            return uoM;
        }
    }
}
