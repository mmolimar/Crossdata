/*
 * Licensed to STRATIO (C) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.  The STRATIO (C) licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.stratio.crossdata.core.normalizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.stratio.crossdata.common.data.ColumnName;
import com.stratio.crossdata.common.data.IndexName;
import com.stratio.crossdata.common.data.TableName;
import com.stratio.crossdata.common.exceptions.ValidationException;
import com.stratio.crossdata.common.exceptions.validation.AmbiguousNameException;
import com.stratio.crossdata.common.exceptions.validation.BadFormatException;
import com.stratio.crossdata.common.exceptions.validation.NotExistNameException;
import com.stratio.crossdata.common.exceptions.validation.NotMatchDataTypeException;
import com.stratio.crossdata.common.exceptions.validation.NotValidCatalogException;
import com.stratio.crossdata.common.exceptions.validation.NotValidColumnException;
import com.stratio.crossdata.common.exceptions.validation.NotValidTableException;
import com.stratio.crossdata.common.exceptions.validation.YodaConditionException;
import com.stratio.crossdata.common.metadata.ColumnMetadata;
import com.stratio.crossdata.common.metadata.ColumnType;
import com.stratio.crossdata.common.metadata.DataType;
import com.stratio.crossdata.common.metadata.IndexMetadata;
import com.stratio.crossdata.common.metadata.IndexType;
import com.stratio.crossdata.common.metadata.TableMetadata;
import com.stratio.crossdata.common.statements.structures.ColumnSelector;
import com.stratio.crossdata.common.statements.structures.FunctionSelector;
import com.stratio.crossdata.common.statements.structures.Operator;
import com.stratio.crossdata.common.statements.structures.OrderByClause;
import com.stratio.crossdata.common.statements.structures.Relation;
import com.stratio.crossdata.common.statements.structures.SelectExpression;
import com.stratio.crossdata.common.statements.structures.Selector;
import com.stratio.crossdata.common.statements.structures.SelectorType;
import com.stratio.crossdata.common.statements.structures.RelationSelector;
import com.stratio.crossdata.common.utils.Constants;
import com.stratio.crossdata.core.metadata.MetadataManager;
import com.stratio.crossdata.core.query.SelectParsedQuery;
import com.stratio.crossdata.core.statements.SelectStatement;
import com.stratio.crossdata.core.structures.GroupByClause;
import com.stratio.crossdata.core.structures.InnerJoin;

/**
 * Normalizator Class.
 */
public class Normalizator {

    private NormalizedFields fields = new NormalizedFields();

    private SelectParsedQuery parsedQuery;

    private Normalizator subqueryNormalizator;


    /**
     * Class Constructor.
     *
     * @param parsedQuery The parsed query.
     */
    public Normalizator(SelectParsedQuery parsedQuery) {
        this.parsedQuery = parsedQuery;
    }

    /**
     * Get the parsed query to Normalize.
     *
     * @return com.stratio.crossdata.core.query.SelectParsedQuery;
     */
    public SelectParsedQuery getParsedQuery() {
        return parsedQuery;
    }

    /**
     * Get the Normalizator of the subquery.
     *
     * @return com.stratio.crossdata.core.normalizer.Normalizator;
     */
    public Normalizator getSubqueryNormalizator() {
        return subqueryNormalizator;
    }

    /**
     * Get the obtained fields normalized.
     *
     * @return com.stratio.crossdata.core.normalizer.NormalizerFields
     */
    public NormalizedFields getFields() {
        return fields;
    }

    /**
     * Execute the normalization of a parsed query.
     *
     * @throws ValidationException
     */
    public void execute() throws ValidationException {

        if (parsedQuery.getStatement().isSubqueryInc()) {
            subqueryNormalizator = new Normalizator(parsedQuery.getChildParsedQuery());
            subqueryNormalizator.execute();
            checkSubquerySelectors(subqueryNormalizator.getFields().getSelectors());
        }

        normalizeTables();
        normalizeSelectExpression();
        normalizeJoins();
        normalizeWhere();
        normalizeOrderBy();
        normalizeGroupBy();
        validateColumnsScope();

    }


    private void checkSubquerySelectors(List<Selector> selectors) throws ValidationException {
        Set<String> uniqueSelectors = new HashSet<>();

        for (Selector selector : selectors) {
            if(selector.getAlias() != null){
                    if(!uniqueSelectors.add(selector.getAlias())){
                        throw new AmbiguousNameException("The alias [" + selector.getAlias() + "] is duplicate");
                    }
            }else if(selector instanceof  ColumnSelector) {
                if(!uniqueSelectors.add(selector.getColumnName().getName())){
                    throw new AmbiguousNameException("The selector [" + selector.getColumnName().getName() + "] is ambiguous. Try using alias");
                }
            }else{
                if(!uniqueSelectors.add(selector.getStringValue())){
                    throw new AmbiguousNameException("The selector [" + selector.getStringValue() + "] is duplicate. Try using alias");
                }
            }
        }

    }

    /**
     * Normalize the tables of a query.
     *
     * @throws ValidationException
     */
    public void normalizeTables() throws ValidationException {
        List<TableName> tableNames = ((SelectStatement) parsedQuery.getStatement()).getAllTables();
        if (tableNames != null && !tableNames.isEmpty()) {
            normalizeTables(tableNames);
        }
    }

    /**
     * Normalize the "from" tables of a parsed query.
     *
     * @param fromTables List of the from clause tables of a parsed query
     * @throws ValidationException
     */
    public void normalizeTables(List<TableName> fromTables) throws ValidationException {
        for (TableName tableName : fromTables) {
            if(!tableName.isVirtual()) {
                checkTable(tableName);
            }
            fields.getCatalogNames().add(tableName.getCatalogName());
            fields.addTableName(tableName);
        }
    }

    /**
     * Normalize all the joins of a parsed query.
     *
     * @throws ValidationException
     */
    public void normalizeJoins() throws ValidationException {
        List<InnerJoin> innerJoinList =  parsedQuery.getStatement().getJoinList();

        if (!innerJoinList.isEmpty()) {
            for(InnerJoin innerJoin: innerJoinList) {
                normalizeJoins(innerJoin);
                fields.addJoin(innerJoin);
            }
        }
    }

    /**
     * Normalize a specific inner join of a parsed query.
     *
     * @param innerJoin The inner join
     * @throws ValidationException
     */
    public void normalizeJoins(InnerJoin innerJoin) throws ValidationException {
        checkJoinRelations(innerJoin.getRelations());
    }

    private void normalizeWhere() throws ValidationException {
        List<Relation> where = ((SelectStatement) parsedQuery.getStatement()).getWhere();
        if (where != null && !where.isEmpty()) {
            normalizeWhere(where);
            fields.setWhere(where);
        }
    }

    private void normalizeWhere(List<Relation> where) throws ValidationException {
        checkWhereRelations(where);
    }

    /**
     * Normalize the order by clause of a parsed query.
     *
     * @throws ValidationException
     */
    public void normalizeOrderBy() throws ValidationException {

        List<OrderByClause> orderByClauseClauses = ((SelectStatement) parsedQuery.getStatement()).getOrderByClauses();

        if (orderByClauseClauses != null) {
            normalizeOrderBy(orderByClauseClauses);
            fields.setOrderByClauses(orderByClauseClauses);
        }
    }

    /**
     * Normalize an specific order by of a parsed query.
     *
     * @param orderByClauseClauses The order by
     * @throws ValidationException
     */
    public void normalizeOrderBy(List<OrderByClause> orderByClauseClauses) throws ValidationException {
        for (OrderByClause orderBy : orderByClauseClauses) {
            Selector selector = orderBy.getSelector();
            switch (selector.getType()) {
            case COLUMN:
                checkColumnSelector((ColumnSelector) selector);
                break;
            case FUNCTION:
            case ASTERISK:
            case BOOLEAN:
            case STRING:
            case INTEGER:
            case FLOATING_POINT:
                throw new BadFormatException("Order by only accepts columns");
            }

        }
    }

    /**
     * Normalize de select clause of a parsed query.
     *
     * @throws ValidationException
     */
    public void normalizeSelectExpression() throws ValidationException {
        List<Pair> selectExpressions = parsedQuery.getStatement().getAllSelectExpressions();
        if (selectExpressions != null) {
            for(Pair<SelectExpression, List<TableName>> pair: selectExpressions){
                normalizeSelectExpression(pair.getLeft(), pair.getRight());
            }
        }
    }

    /**
     * Normalize an specific select expression.
     *
     * @param selectExpression The select expression
     * @throws ValidationException
     */
    public void normalizeSelectExpression(SelectExpression selectExpression,
            List<TableName> preferredTables) throws ValidationException {
        List<Selector> normalizeSelectors = checkListSelector(selectExpression.getSelectorList(), preferredTables);
        fields.getSelectors().addAll(normalizeSelectors);
        selectExpression.getSelectorList().clear();
        selectExpression.getSelectorList().addAll(normalizeSelectors);
    }

    /**
     * Normalize the group by of a parsed query.
     *
     * @throws ValidationException
     */
    public void normalizeGroupBy() throws ValidationException {
        GroupByClause groupByClause = ((SelectStatement) parsedQuery.getStatement()).getGroupByClause();
        if (groupByClause != null) {
            normalizeGroupBy(groupByClause);
            fields.setGroupByClause(groupByClause);
        }
    }

    private void checkFormatBySelectorIdentifier(Selector selector, Set<ColumnName> columnNames)
                    throws ValidationException {
        switch (selector.getType()) {
        case FUNCTION:
            throw new BadFormatException("Function include into groupBy is not valid");
        case COLUMN:
            checkColumnSelector((ColumnSelector) selector);
            if (!columnNames.add(((ColumnSelector) selector).getName())) {
                throw new BadFormatException("COLUMN into group by is repeated");
            }
            break;
        case ASTERISK:
            throw new BadFormatException("Asterisk include into groupBy is not valid");
        }
    }

    private void checkGroupByColumns(Selector selector, Set<ColumnName> columnNames) throws BadFormatException {
        switch (selector.getType()) {
        case FUNCTION:
            break;
        case COLUMN:
            ColumnName name = ((ColumnSelector) selector).getName();
            if (!columnNames.contains(name)) {
                throw new BadFormatException(
                                "All columns in the select clause must be in the group by or it must be aggregation includes.");
            }
            break;
        case ASTERISK:
            throw new BadFormatException("Asterisk is not valid with group by statements");
        }
    }

    /**
     * Normalize an specific group by of a parsed query.
     *
     * @param groupByClause
     * @throws ValidationException
     */
    public void normalizeGroupBy(GroupByClause groupByClause) throws ValidationException {
        Set<ColumnName> columnNames = new HashSet<>();
        for (Selector selector : groupByClause.getSelectorIdentifier()) {
            checkFormatBySelectorIdentifier(selector, columnNames);
        }
        // Check if all columns are correct
        for (Selector selector : fields.getSelectors()) {
            checkGroupByColumns(selector, columnNames);
        }
    }

    private void validateColumnsScope() throws ValidationException {
        for (ColumnName columnName : fields.getColumnNames()) {
            String expectedTableName = columnName.getTableName().getQualifiedName();
            Iterator<TableName> tableNamesIterator = fields.getTableNames().iterator();
            boolean tableFound = false;
            while (!tableFound && tableNamesIterator.hasNext()) {
                tableFound = tableNamesIterator.next().getQualifiedName().equals(expectedTableName);
            }
            if (!tableFound) {
                throw new NotValidTableException("The table [" + expectedTableName + "] is not within the scope of the query");
            }
        }
    }

    /**
     * Validate the joins of a parsed query.
     *
     * @param relations A list of Relation to check.
     * @throws ValidationException
     */
    public void checkJoinRelations(List<Relation> relations) throws ValidationException {
        for (Relation relation : relations) {
            checkRelation(relation);
            switch (relation.getOperator()) {
            case EQ:
                if (relation.getLeftTerm().getType() == SelectorType.COLUMN
                                && relation.getRightTerm().getType() == SelectorType.COLUMN) {

                    checkColumnSelector((ColumnSelector) relation.getRightTerm());
                    checkColumnSelector((ColumnSelector) relation.getLeftTerm());
                } else {
                    throw new BadFormatException("You must compare between columns");
                }
                break;
            default:
                throw new BadFormatException("Only equal operation are just valid");
            }
        }
    }

    /**
     * Validate the where clause of a parsed query.
     *
     * @param relations The list of Relation that contains the where clause to check
     * @throws ValidationException
     */
    public void checkWhereRelations(List<Relation> relations) throws ValidationException {
        for (Relation relation : relations) {
            checkRelation(relation);
        }
    }

    /**
     * Validate the relation of a parsed query.
     *
     * @param relation The relation of the query.
     * @throws ValidationException
     */
    public void checkRelation(Relation relation) throws ValidationException {
        if (relation.getOperator().isInGroup(Operator.Group.ARITHMETIC)) {
            throw new BadFormatException("Compare operations are just valid");
        }
        checkRelationFormatLeft(relation);
        checkRelationFormatRight(relation);
    }

    private void checkRelationFormatLeft(Relation relation) throws ValidationException {
        switch (relation.getLeftTerm().getType()) {
        case FUNCTION:
            throw new BadFormatException("Functions not supported yet");
        case COLUMN:
            checkColumnSelector((ColumnSelector) relation.getLeftTerm());
            break;
        case ASTERISK:
            throw new BadFormatException("Asterisk not supported in relations.");
        case STRING:
        case FLOATING_POINT:
        case BOOLEAN:
        case INTEGER:
            throw new YodaConditionException();
        }
    }

    private void checkRelationFormatRight(Relation relation) throws ValidationException {
        switch (relation.getRightTerm().getType()) {
        case COLUMN:
        case STRING:
        case FLOATING_POINT:
        case BOOLEAN:
        case INTEGER:
            ColumnSelector columnSelector = (ColumnSelector) relation.getLeftTerm();
            checkRightSelector(columnSelector.getName(), relation.getOperator(), relation.getRightTerm());
            break;
        case RELATION:
        case ASTERISK:
            throw new BadFormatException("Not supported yet.");
        case FUNCTION:
            break;

        }
    }

    /**
     * Validate the table of a parsed query.
     *
     * @param tableName The table name to validate
     * @throws ValidationException
     */
    public void checkTable(TableName tableName) throws ValidationException {
        if (!tableName.isCompletedName()) {
            tableName.setCatalogName(parsedQuery.getDefaultCatalog());
        }
        if (!MetadataManager.MANAGER.exists(tableName)) {
            throw new NotExistNameException(tableName);
        }
    }

    /**
     * Validate the column selectors.
     *
     * @param selector The selector
     * @throws ValidationException
     */
    public void checkColumnSelector(ColumnSelector selector) throws ValidationException {
        ColumnName columnName = applyAlias(selector.getName());
        boolean columnFromVirtualTableFound = false;

        if(parsedQuery.getStatement().isSubqueryInc() ) {
            //when the name is not completed and the table is not virtual
            if(!columnName.isCompletedName() || columnName.getTableName().isVirtual()) {
                columnFromVirtualTableFound = checkVirtualColumnSelector(selector, columnName);
            }
        }

        if(!columnFromVirtualTableFound) {
            if (columnName.isCompletedName()) {
                if (!MetadataManager.MANAGER.exists(columnName)) {
                    throw new NotValidColumnException(columnName);
                }
            } else {
                TableName searched = this.searchTableNameByColumn(columnName);
                columnName.setTableName(searched);
            }
        }

        fields.addColumnName(columnName, selector.getAlias());
        selector.setName(columnName);

    }

    private boolean checkVirtualColumnSelector(ColumnSelector selector, ColumnName columnName) throws NotValidTableException {

        boolean columnFromVirtualTableFound = false;
        TableName tableName;

        if (columnName.getTableName() != null) {
            tableName = fields.getTableName(columnName.getTableName().getName());
            if (tableName == null) {
                throw new NotValidTableException(columnName.getTableName());
            }
        }else {
            tableName = new TableName(Constants.VIRTUAL_CATALOG_NAME,parsedQuery.getStatement().getSubqueryAlias());
        }

        for (Selector subquerySelector : subqueryNormalizator.getFields().getSelectors()) {

            if (subquerySelector.getAlias() != null) {
                columnFromVirtualTableFound = selector.getName().getName().equals(subquerySelector.getAlias());
            } else if (subquerySelector instanceof ColumnSelector) {
                columnFromVirtualTableFound = selector.getColumnName().getName()
                                .equals(subquerySelector.getColumnName().getName());
            }

            if (columnFromVirtualTableFound) {
                columnName.setTableName(tableName);
                selector.setTableName(tableName);
                columnFromVirtualTableFound = true;
                break;
            }
        }

        return columnFromVirtualTableFound;


        /*boolean columnFromVirtualTableFound = false;
        TableName tableName;

        if (columnName.getTableName() != null) {
            tableName = fields.getTableName(columnName.getTableName().getName());
            if(tableName == null){
                throw new NotValidTableException(columnName.getTableName());
            }
            columnName.setTableName(tableName);
            selector.setTableName(tableName);
            columnFromVirtualTableFound = true;
            //TODO validate with the subquery, reuse the code below
        } else {

            for (Selector subquerySelector : subqueryNormalizator.getFields().getSelectors()) {
                if(subquerySelector.getAlias() != null){
                    columnFromVirtualTableFound = selector.getName().getName().equals(subquerySelector.getAlias());
                }else if(subquerySelector instanceof ColumnSelector){
                    columnFromVirtualTableFound = selector.getColumnName().getName().equals(subquerySelector.getColumnName().getName());
                }

                if (columnFromVirtualTableFound) {
                    tableName = new TableName(Constants.VIRTUAL_CATALOG_NAME,parsedQuery.getStatement().getSubqueryAlias());
                    columnName.setTableName(tableName);
                    selector.setTableName(tableName);
                    columnFromVirtualTableFound = true;
                    break;
                }

            }

        }
        return columnFromVirtualTableFound;*/
    }

    private ColumnName applyAlias(ColumnName columnName) {
        ColumnName result = columnName;
        if (columnName.getTableName() != null && fields.existTableAlias(columnName.getTableName().getName())) {
            columnName.setTableName(fields.getTableName(columnName.getTableName().getName()));
        }

        if (fields.existColumnAlias(columnName.getName())) {
            result = fields.getColumnName(columnName.getName());
        }
        return result;
    }

    /**
     * Search a table using a column name.
     *
     * @param columnName The column name
     * @return com.stratio.crossdata.common.data.ColumnName
     * @throws ValidationException
     */
    public TableName searchTableNameByColumn(ColumnName columnName) throws ValidationException {
        TableName selectTableName = null;
        if (columnName.isCompletedName()) {
            if (MetadataManager.MANAGER.exists(columnName)) {
                selectTableName = columnName.getTableName();
            }
        } else {
            if (columnName.getTableName() == null) {
                boolean tableFind = false;
                for (TableName tableName: fields.getTableNames()) {
                    columnName.setTableName(tableName);
                    if (MetadataManager.MANAGER.exists(columnName)) {
                        if (tableFind) {
                            throw new AmbiguousNameException(columnName);
                        }
                        selectTableName = tableName;
                        tableFind = true;
                    } else {
                        columnName.setTableName(null);

                    }
                }
            } else {
                for (TableName tableName : fields.getTableNames()) {
                    if (tableName.getName().equals(columnName.getTableName().getName())) {
                        columnName.setTableName(tableName);
                        selectTableName = tableName;
                    }
                }
                if (!columnName.isCompletedName()) {
                    throw new NotValidColumnException(columnName);
                }
            }
        }
        if (selectTableName == null) {
            throw new NotExistNameException(columnName);
        }
        return selectTableName;
    }

    /**
     * Validate the conditions that have an asterisk.
     *
     * @return List of ColumnSelector
     */
    public List<Selector> checkAsteriskSelector() throws ValidationException{
        List<Selector> aSelectors = new ArrayList<>();
        SelectStatement selectStatement = parsedQuery.getStatement();
        for (TableName table : fields.getTableNames()) {

            if (table.isVirtual()) {
                for (Selector selector : selectStatement.getSubquery().getSelectExpression().getSelectorList()) {
                    ColumnName colName = new ColumnName(table,getVirtualAliasFromSelector(selector));
                    Selector defaultSelector = new ColumnSelector(colName);
                    defaultSelector.setAlias(selector.getAlias());
                    defaultSelector.setTableName(selectStatement.getTableName());
                    aSelectors.add(defaultSelector);
                    fields.addColumnName(colName, defaultSelector.getAlias());
                }

            } else {
                TableMetadata tableMetadata = MetadataManager.MANAGER.getTable(table);
                for (ColumnName columnName : tableMetadata.getColumns().keySet()) {
                    ColumnSelector selector = new ColumnSelector(columnName);
                    aSelectors.add(selector);
                    fields.getColumnNames().add(columnName);
                }
            }

        }
        return aSelectors;
    }

    private String getVirtualAliasFromSelector(Selector selector) {
        String strColName;
        if(selector.getAlias() != null){
            strColName = selector.getAlias();
        }else if(selector instanceof ColumnSelector) {
            strColName = selector.getColumnName().getName();
        }else{
            strColName = selector.getStringValue();
        }
        return strColName;
    }

    /**
     * Obtain a list of selectors that were validated.
     *
     * @param selectors The list of selectors to validate.
     * @return List of Selectors
     * @throws ValidationException
     */
    public List<Selector> checkListSelector(List<Selector> selectors,
            List<TableName> preferredTables) throws ValidationException {
        List<Selector> result = new ArrayList<>();
        //TableName firstTableName = fields.getTableNames().iterator().next();
        TableName firstTableName = preferredTables.get(0);
        for (Selector selector : selectors) {
            switch (selector.getType()) {
            case FUNCTION:
                FunctionSelector functionSelector = (FunctionSelector) selector;
                checkFunctionSelector(functionSelector, preferredTables);
                functionSelector.setTableName(firstTableName);
                result.add(functionSelector);
                break;
            case COLUMN:
                ColumnSelector columnSelector = (ColumnSelector) selector;
                checkColumnSelector(columnSelector);

                //check with selectFromTables to add the secondTableName
                //Iterator<TableName> tableNameIterator = fields.getTableNames().iterator();
                Iterator<TableName> tableNameIterator = preferredTables.iterator();

                TableName currentTableName = null;
                boolean tableFound=false;
                while (tableNameIterator.hasNext() && !tableFound){
                    currentTableName = tableNameIterator.next();
                    if(columnSelector.getTableName() != null) {
                        if (!columnSelector.getName().getTableName().getName().equals(currentTableName.getName()) && ! tableNameIterator.hasNext()) {
                            throw new NotValidTableException(columnSelector.getName().getTableName());
                        }else{

                            tableFound = ! (columnSelector.getName().getTableName().getCatalogName() != null && !columnSelector.getName().getTableName().getCatalogName().getName().equals(currentTableName.getCatalogName().getName()) );
                            if (!tableFound && ! tableNameIterator.hasNext()) {
                                throw new NotValidCatalogException(columnSelector.getTableName().getCatalogName());
                            }
                        }
                    }
                }
                columnSelector.setTableName(currentTableName);

                result.add(columnSelector);
                break;
            case ASTERISK:
                result.addAll(checkAsteriskSelector());
                break;
            default:
                Selector defaultSelector = selector;
                defaultSelector.setTableName(firstTableName);
                result.add(defaultSelector);
                break;
            }
        }
        return result;
    }

    /**
     * Validate the Functions Selectors of a parsed query.
     *
     *
     * @param functionSelector The includes Selector to validate.
     * @param preferredTables
     * @throws ValidationException
     */
    private void checkFunctionSelector(FunctionSelector functionSelector, List<TableName> preferredTables) throws ValidationException {
        // Check columns
        List<Selector> normalizeSelector = checkListSelector(functionSelector.getFunctionColumns(), preferredTables);
        functionSelector.getFunctionColumns().clear();
        functionSelector.getFunctionColumns().addAll(normalizeSelector);
    }

    private void checkRightSelector(ColumnName name, Operator operator, Selector rightTerm) throws ValidationException {

        if(!parsedQuery.getStatement().isSubqueryInc()) {
            // Get column type from MetadataManager
            ColumnMetadata columnMetadata = MetadataManager.MANAGER.getColumn(name);
            SelectorType rightTermType = rightTerm.getType();

            if (rightTerm.getType() == SelectorType.COLUMN) {
                ColumnSelector columnSelector = (ColumnSelector) rightTerm;
                ColumnName columnName = applyAlias(columnSelector.getName());
                columnSelector.setName(columnName);

                TableName foundTableName = this.searchTableNameByColumn(columnSelector.getName());
                columnSelector.getName().setTableName(foundTableName);

                ColumnMetadata columnMetadataRightTerm = MetadataManager.MANAGER.getColumn(columnSelector.getName());

                if (columnMetadataRightTerm.getColumnType().getDataType() != DataType.NATIVE) {
                    rightTermType = convertMetadataTypeToSelectorType(columnMetadataRightTerm.getColumnType());
                }
            }
            // Create compatibilities table for ColumnType, Operator and SelectorType
            if (operator!=Operator.MATCH){
                checkCompatibility(columnMetadata, operator, rightTermType);
            }
        }

    }

    private SelectorType convertMetadataTypeToSelectorType(ColumnType columnType) throws ValidationException {
        SelectorType selectorType = null;
        switch (columnType.getDataType()) {
        case INT:
        case BIGINT:
            selectorType = SelectorType.INTEGER;
            break;
        case DOUBLE:
        case FLOAT:
            selectorType = SelectorType.FLOATING_POINT;
            break;
        case TEXT:
        case VARCHAR:
            selectorType = SelectorType.STRING;
            break;
        case BOOLEAN:
            selectorType = SelectorType.BOOLEAN;
            break;
        case NATIVE:
        case SET:
        case LIST:
        case MAP:
            throw new BadFormatException("Type " + columnType + " not supported yet.");
        }
        return selectorType;
    }

    private void checkCompatibility(ColumnMetadata column, Operator operator, SelectorType valueType)
                    throws ValidationException {
        switch (column.getColumnType().getDataType()) {
        case BOOLEAN:
            checkBooleanCompatibility(column, operator, valueType);
            break;
        case INT:
        case BIGINT:
        case DOUBLE:
        case FLOAT:
            checkNumericCompatibility(column, valueType);
            break;
        case TEXT:
        case VARCHAR:
            checkStringCompatibility(column, operator, valueType);
            break;
        case SET:
        case LIST:
        case MAP:
            throw new BadFormatException("Native and Collections not supported yet.");
        case NATIVE:
            //we don't check native types
            break;
        }
    }

    private void checkBooleanCompatibility(ColumnMetadata column, Operator operator, SelectorType valueType)
                    throws ValidationException {
        if (operator != Operator.EQ) {
            throw new BadFormatException("Boolean relations only accept equal operator.");
        }
        if (valueType != SelectorType.BOOLEAN) {
            throw new NotMatchDataTypeException(column.getName());
        }
    }

    private void checkNumericCompatibility(ColumnMetadata column, SelectorType valueType) throws ValidationException {
        if ((valueType != SelectorType.INTEGER) && (valueType != SelectorType.FLOATING_POINT)) {
            throw new NotMatchDataTypeException(column.getName());
        }
    }

    private void checkStringCompatibility(ColumnMetadata column, Operator operator, SelectorType valueType)
                    throws ValidationException {
        if (valueType != SelectorType.STRING) {
            throw new NotMatchDataTypeException(column.getName());
        }

        if (operator == Operator.MATCH) {
            if (valueType != SelectorType.STRING) {
                throw new BadFormatException("MATCH operator only accepts comparison with string literals.");
            }

            TableMetadata tableMetadata = MetadataManager.MANAGER.getTable(column.getName().getTableName());
            Map<IndexName, IndexMetadata> indexes = tableMetadata.getIndexes();
            if (indexes == null || indexes.isEmpty()) {
                throw new BadFormatException(
                                "Table " + column.getName().getTableName() + " doesn't contain any index.");
            }

            boolean indexFound = false;
            Collection<IndexMetadata> indexesMetadata = indexes.values();
            Iterator<IndexMetadata> iter = indexesMetadata.iterator();
            while (iter.hasNext() && !indexFound) {
                IndexMetadata indexMetadata = iter.next();
                if (indexMetadata.getColumns().containsKey(column.getName())) {
                    if (indexMetadata.getType() != IndexType.FULL_TEXT) {
                        throw new BadFormatException("MATCH operator can be only applied to FULL_TEXT indexes.");
                    } else {
                        indexFound = true;
                    }
                }
            }
            if (!indexFound) {
                throw new BadFormatException("No index was found for the MATCH operator.");
            }
        } else if ((operator != Operator.EQ) && (operator != Operator.GT) && (operator != Operator.GET) && (operator
                        != Operator.LT) && (operator != Operator.LET) && (operator != Operator.DISTINCT)) {
            throw new BadFormatException("String relations only accept equal operator.");
        }

    }
}
