/**
 * Copyright (C) 2014 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.actorfilter.identity;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.bonitasoft.actorfilter.identity.SameTaskUserActorFilter.USERTASK_NAME;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.flownode.ArchivedHumanTaskInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedHumanTaskInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.impl.internal.ArchivedUserTaskInstanceImpl;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.bonitasoft.engine.connector.EngineExecutionContext;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.filter.UserFilterException;
import org.bonitasoft.engine.search.SearchOptions;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.search.impl.SearchOptionsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class SameTaskUserActorFilterTest {

    private static final String HUMAN_TASK_NAME = "step1";

    private static final long PROCESS_INSTANCE_ID = 83L;

    @Mock(lenient = true)
    private APIAccessor apiAccessor;

    @Mock(lenient = true)
    private ProcessAPI processAPI;

    @Mock(lenient = true)
    private SearchResult<ArchivedHumanTaskInstance> searchResult;

    @InjectMocks
    private SameTaskUserActorFilter filter;

    @BeforeEach
    public void setUp() {

        when(apiAccessor.getProcessAPI()).thenReturn(processAPI);

        EngineExecutionContext context = new EngineExecutionContext();
        context.setProcessInstanceId(PROCESS_INSTANCE_ID);
        filter.setExecutionContext(context);
    }

    private Map<String, Object> initParameters(final String humanTaskName) {
        return singletonMap(USERTASK_NAME, humanTaskName);
    }

    private List<ArchivedHumanTaskInstance> buildSingleResultList(final long userId) {
        final List<ArchivedHumanTaskInstance> result = new ArrayList<>();
        final ArchivedUserTaskInstanceImpl instanceImpl = new ArchivedUserTaskInstanceImpl(HUMAN_TASK_NAME);
        instanceImpl.setExecutedBy(4786L);
        result.add(instanceImpl);
        return result;
    }

    private List<ArchivedHumanTaskInstance> buildMultipleResultList(final long userId) {
        final List<ArchivedHumanTaskInstance> result = new ArrayList<>();
        ArchivedUserTaskInstanceImpl instanceImpl = new ArchivedUserTaskInstanceImpl(HUMAN_TASK_NAME);
        instanceImpl.setExecutedBy(4786L);
        result.add(instanceImpl);
        instanceImpl = new ArchivedUserTaskInstanceImpl(HUMAN_TASK_NAME);
        instanceImpl.setExecutedBy(4786L);
        result.add(instanceImpl);
        return result;
    }

    private SearchOptionsImpl buildSearchOptionsResult() {
        final SearchOptionsImpl optionsImpl = new SearchOptionsImpl(0, 2000);
        optionsImpl.addFilter(ArchivedHumanTaskInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID, PROCESS_INSTANCE_ID);
        optionsImpl.addFilter(ArchivedHumanTaskInstanceSearchDescriptor.NAME, HUMAN_TASK_NAME);
        optionsImpl.addFilter(ArchivedHumanTaskInstanceSearchDescriptor.TERMINAL, true);
        return optionsImpl;
    }

    @Test
    void validateInputParameters_should_throw_an_exception_due_to_missing_human_task_name() throws Exception {
        assertThrows(ConnectorValidationException.class, () ->
                filter.validateInputParameters()
        );
    }

    @Test
    void validateInputParameters_should_throw_an_exception_bacause_human_task_name_is_null() throws Exception {
        filter.setInputParameters(initParameters(null));

        assertThrows(ConnectorValidationException.class, () ->
                filter.validateInputParameters()
        );
    }

    @Test
    void validateInputParameters_should_throw_an_exception_bacause_human_task_name_is_empty() throws Exception {
        filter.setInputParameters(initParameters("   "));

        assertThrows(ConnectorValidationException.class, () ->
                filter.validateInputParameters()
        );
    }

    @Test
    void validateInputParameters_should_check_that_human_task_name_is_present() throws Exception {
        filter.setInputParameters(initParameters(HUMAN_TASK_NAME));

        filter.validateInputParameters();
    }

    @Test
    void filter_should_return_the_list_of_user_ids() throws Exception {
        final long userId = 4786L;
        filter.setInputParameters(initParameters(HUMAN_TASK_NAME));
        when(processAPI.searchArchivedHumanTasks(any(SearchOptions.class))).thenReturn(searchResult);
        when(searchResult.getCount()).thenReturn(1L);
        final List<ArchivedHumanTaskInstance> result = buildSingleResultList(userId);
        when(searchResult.getResult()).thenReturn(result);
        final SearchOptionsImpl optionsImpl = buildSearchOptionsResult();

        final List<Long> userIds = filter.filter("Employee");

        assertThat(userIds).hasSize(1).contains(userId);
        verify(processAPI).searchArchivedHumanTasks(optionsImpl);
    }

    @Test
    void filter_should_return_the_list_of_user_ids_without_duplicate() throws Exception {
        final long userId = 4786L;
        filter.setInputParameters(initParameters(HUMAN_TASK_NAME));
        when(processAPI.searchArchivedHumanTasks(any(SearchOptions.class))).thenReturn(searchResult);
        when(searchResult.getCount()).thenReturn(2L);
        final List<ArchivedHumanTaskInstance> result = buildMultipleResultList(userId);
        when(searchResult.getResult()).thenReturn(result);
        final SearchOptionsImpl optionsImpl = buildSearchOptionsResult();

        final List<Long> userIds = filter.filter("Employee");

        assertThat(userIds).hasSize(1).contains(userId);
        verify(processAPI).searchArchivedHumanTasks(optionsImpl);
    }

    @Test
    void filter_should_throw_an_exception_if_the_api_call_throws_an_exception() throws Exception {
        when(processAPI.searchArchivedHumanTasks(any(SearchOptions.class))).thenThrow(new SearchException(null));

        assertThrows(UserFilterException.class, () ->
                filter.filter("Employee")
        );

    }

    @Test
    void filter_should_throw_an_exception_if_the_search_result_is_empty() throws Exception {
        when(processAPI.searchArchivedHumanTasks(any(SearchOptions.class))).thenReturn(searchResult);
        when(searchResult.getCount()).thenReturn(0L);

        assertThrows(UserFilterException.class, () ->
                filter.filter("Employee")
        );
    }

}
