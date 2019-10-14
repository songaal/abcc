package org.fastcatsearch.http.action.management.dictionary;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.fastcatsearch.db.dao.DictionaryDAO;
import org.fastcatsearch.http.ActionAuthority;
import org.fastcatsearch.http.ActionAuthorityLevel;
import org.fastcatsearch.http.ActionMapping;
import org.fastcatsearch.http.action.ActionRequest;
import org.fastcatsearch.http.action.ActionResponse;
import org.fastcatsearch.http.action.AuthAction;
import org.fastcatsearch.http.action.service.dictionary.GetDictionaryWordListBase;
import org.fastcatsearch.plugin.Plugin;
import org.fastcatsearch.plugin.PluginService;
import org.fastcatsearch.plugin.analysis.AnalysisPlugin;
import org.fastcatsearch.plugin.analysis.AnalysisPluginSetting;
import org.fastcatsearch.plugin.analysis.AnalysisPluginSetting.ColumnSetting;
import org.fastcatsearch.plugin.analysis.AnalysisPluginSetting.DictionarySetting;
import org.fastcatsearch.service.ServiceManager;
import org.fastcatsearch.util.ResponseWriter;

@ActionMapping(value="/management/dictionary/list", authority=ActionAuthority.Dictionary, authorityLevel=ActionAuthorityLevel.READABLE)
public class GetDictionaryWordListAction extends AuthAction {

    @Override
    public void doAuthAction(ActionRequest request, ActionResponse response) throws Exception {
        Writer writer = response.getWriter();
        ResponseWriter resultWriter = getDefaultResponseWriter(writer);
        new GetDictionaryWordListBase().doAction(request, resultWriter);
    }

}
