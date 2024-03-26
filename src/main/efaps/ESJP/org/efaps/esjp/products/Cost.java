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

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_base</code>"
 * class.
 *
 * @author The eFaps Team
 */
@EFapsUUID("f25ae69a-e283-4036-8326-47e61ff768d4")
@EFapsApplication("eFapsApp-Products")
public class Cost
    extends Cost_Base
{
    /** The Constant CACHKEY. */
    public static final String CACHKEY = Cost_Base.CACHKEY;

    /**
     * Gets the cost4 currency.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _productInstance the product instance
     * @param _currencyInstance the currency instance
     * @return the cost4 currency
     * @throws EFapsException on error
     */
    public static BigDecimal getCost4Currency(final Parameter _parameter,
                                              final Instance _productInstance,
                                              final Instance _currencyInstance)
        throws EFapsException
    {
        return Cost_Base.getCost4Currency(_parameter, _productInstance, _currencyInstance);
    }

    /**
     * Gets the cost4 currency.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _date the date
     * @param _productInstance the product instance
     * @param _currencyInstance the currency instance
     * @return the cost4 currency
     * @throws EFapsException on error
     */
    public static BigDecimal getCost4Currency(final Parameter _parameter,
                                              final DateTime _date,
                                              final Instance _productInstance,
                                              final Instance _currencyInstance)
        throws EFapsException
    {
        return Cost_Base.getCost4Currency(_parameter, _date, _productInstance, _currencyInstance);
    }

    /**
     * Gets the cost4 currency.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _alterCurrencyInstance the alter currency instance
     * @param _productInstance the product instance
     * @param _currencyInstance the currency instance
     * @return the cost4 currency
     * @throws EFapsException on error
     */
    public static BigDecimal getAlternativeCost4Currency(final Parameter _parameter,
                                                         final Instance _alterCurrencyInstance,
                                                         final Instance _productInstance,
                                                         final Instance _currencyInstance)
        throws EFapsException
    {
        return Cost_Base.getAlternativeCost4Currency(_parameter, _alterCurrencyInstance, _productInstance,
                        _currencyInstance);
    }

    /**
     * Gets the cost4 currency.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _date the date
     * @param _alterCurrencyInstance the alter currency instance
     * @param _productInstance the product instance
     * @param _currencyInstance the currency instance
     * @return the cost4 currency
     * @throws EFapsException on error
     */
    public static BigDecimal getAlternativeCost4Currency(final Parameter _parameter,
                                                         final DateTime _date,
                                                         final Instance _alterCurrencyInstance,
                                                         final Instance _productInstance,
                                                         final Instance _currencyInstance)
        throws EFapsException
    {
        return Cost_Base.getAlternativeCost4Currency(_parameter, _date, _alterCurrencyInstance, _productInstance,
                        _currencyInstance);
    }

    /**
     * Gets the cost4 currency.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _date the date
     * @param _requestDate the request date
     * @param _productInstance the product instance
     * @param _currencyInstance the currency instance
     * @return the cost4 currency
     * @throws EFapsException on error
     */
    public static BigDecimal getHistoricCost4Currency(final Parameter _parameter,
                                                      final DateTime _date,
                                                      final DateTime _requestDate,
                                                      final Instance _productInstance,
                                                      final Instance _currencyInstance)
        throws EFapsException
    {
        return Cost_Base.getHistoricCost4Currency(_parameter, _date, _requestDate, _productInstance, _currencyInstance);
    }

    /**
     * Gets the cost4 currency.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _date the date
     * @param _requestDate the request date
     * @param _alterCurrencyInstance the alter currency instance
     * @param _productInstance the product instance
     * @param _currencyInstance the currency instance
     * @return the cost4 currency
     * @throws EFapsException on error
     */
    public static BigDecimal getHistoricAlternativeCost4Currency(final Parameter _parameter,
                                                                 final DateTime _date,
                                                                 final DateTime _requestDate,
                                                                 final Instance _alterCurrencyInstance,
                                                                 final Instance _productInstance,
                                                                 final Instance _currencyInstance)
        throws EFapsException
    {
        return Cost_Base.getHistoricAlternativeCost4Currency(_parameter, _date, _requestDate, _alterCurrencyInstance,
                        _productInstance, _currencyInstance);
    }
}
