package org.bonitasoft.actorfilter.identity;

import org.bonitasoft.engine.bpm.flownode.ArchivedHumanTaskInstance;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.filter.AbstractUserFilter;
import org.bonitasoft.engine.filter.UserFilterException;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.util.Collections.unmodifiableList;
import static org.bonitasoft.engine.bpm.flownode.ArchivedHumanTaskInstanceSearchDescriptor.*;

/**
 * Filters the userId(s) to the ones of the user(s) who executed a task specified by its name.
 * If a large number of task instances have the provided name for the running process, only the first 2000 results are returned.
 *
 * @author Emmanuel Duchastenier
 * @author Matthieu Chaffotte
 */
public class SameTaskUserActorFilter extends AbstractUserFilter {

    static final String USERTASK_NAME = "usertaskName";

    private static final Logger LOGGER = Logger.getLogger(SameTaskUserActorFilter.class.getName());

    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        validateStringInputParameterIsNotNulOrEmpty("usertaskName");
    }

    @Override
    public List<Long> filter(final String actorName) throws UserFilterException {
        final String usertaskName = (String) getInputParameter(USERTASK_NAME);

        final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 2000)
                .filter(PARENT_PROCESS_INSTANCE_ID, getExecutionContext().getProcessInstanceId())
                .filter(NAME, usertaskName).filter(TERMINAL, true);

        SearchResult<ArchivedHumanTaskInstance> searchResult;
        try {
            searchResult = getAPIAccessor().getProcessAPI().searchArchivedHumanTasks(searchOptionsBuilder.done());
        } catch (final SearchException e) {
            throw new UserFilterException("Problem searching for task named: " + usertaskName, e);
        }
        if (searchResult.getCount() == 0) {
            throw new UserFilterException("No finished task found with name: " + usertaskName);
        }

        final List<ArchivedHumanTaskInstance> tasks = searchResult.getResult();
        final List<Long> userIds = new ArrayList<Long>(tasks.size());
        for (final ArchivedHumanTaskInstance archivedTask : tasks) {
            final long executorId = archivedTask.getExecutedBy();
            if (!userIds.contains(executorId)) {
                userIds.add(executorId);
            }
        }
        return unmodifiableList(userIds);

    }
}

