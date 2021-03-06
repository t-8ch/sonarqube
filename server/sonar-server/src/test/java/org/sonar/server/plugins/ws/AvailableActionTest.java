/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.plugins.ws;

import com.google.common.base.Optional;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginUpdate;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.sonar.core.permission.GlobalPermissions.DASHBOARD_SHARING;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonar.updatecenter.common.PluginUpdate.Status.COMPATIBLE;
import static org.sonar.updatecenter.common.PluginUpdate.Status.DEPENDENCIES_REQUIRE_SONAR_UPGRADE;
import static org.sonar.updatecenter.common.PluginUpdate.Status.INCOMPATIBLE;
import static org.sonar.updatecenter.common.PluginUpdate.Status.REQUIRE_SONAR_UPGRADE;

public class AvailableActionTest extends AbstractUpdateCenterBasedPluginsWsActionTest {

  private static final Plugin FULL_PROPERTIES_PLUGIN = Plugin.factory("pkey")
    .setName("p_name")
    .setCategory("p_category")
    .setDescription("p_description")
    .setLicense("p_license")
    .setOrganization("p_orga_name")
    .setOrganizationUrl("p_orga_url")
    .setHomepageUrl("p_homepage_url")
    .setIssueTrackerUrl("p_issue_url")
    .setTermsConditionsUrl("p_t_and_c_url");
  private static final Release FULL_PROPERTIES_PLUGIN_RELEASE = release(FULL_PROPERTIES_PLUGIN, "1.12.1")
    .setDate(DateUtils.parseDate("2015-04-16"))
    .setDownloadUrl("http://p_file.jar")
    .addOutgoingDependency(release(PLUGIN_1, "0.3.6"))
    .addOutgoingDependency(release(PLUGIN_2, "1.0.0"));

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AvailableAction underTest = new AvailableAction(userSession, updateCenterFactory, new PluginWSCommons());

  @Test
  public void action_available_is_defined() {
    loggedAsAdmin();
    WsTester wsTester = new WsTester();
    WebService.NewController newController = wsTester.context().createController(DUMMY_CONTROLLER_KEY);

    underTest.define(newController);
    newController.done();

    WebService.Controller controller = wsTester.controller(DUMMY_CONTROLLER_KEY);
    assertThat(controller.actions()).extracting("key").containsExactly("available");

    WebService.Action action = controller.actions().iterator().next();
    assertThat(action.isPost()).isFalse();
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExample()).isNotNull();
  }

  @Test
  public void empty_array_is_returned_when_there_is_no_plugin_available() throws Exception {
    loggedAsAdmin();
    underTest.handle(request, response);

    assertJson(response.outputAsString()).withStrictArrayOrder().isSimilarTo(JSON_EMPTY_PLUGIN_LIST);
  }

  @Test
  public void empty_array_is_returned_when_update_center_is_not_accessible() throws Exception {
    loggedAsAdmin();
    when(updateCenterFactory.getUpdateCenter(anyBoolean())).thenReturn(Optional.<UpdateCenter>absent());

    underTest.handle(request, response);

    assertJson(response.outputAsString()).withStrictArrayOrder().isSimilarTo(JSON_EMPTY_PLUGIN_LIST);
  }

  @Test
  public void verify_properties_displayed_in_json_per_plugin() throws Exception {
    loggedAsAdmin();
    when(updateCenter.findAvailablePlugins()).thenReturn(of(
      pluginUpdate(FULL_PROPERTIES_PLUGIN_RELEASE, COMPATIBLE)
    ));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(resource("properties_per_plugin.json"));
  }

  @Test
  public void status_COMPATIBLE_is_displayed_COMPATIBLE_in_JSON() throws Exception {
    loggedAsAdmin();
    checkStatusDisplayedInJson(COMPATIBLE, "COMPATIBLE");
  }

  @Test
  public void status_INCOMPATIBLE_is_displayed_INCOMPATIBLE_in_JSON() throws Exception {
    loggedAsAdmin();
    checkStatusDisplayedInJson(INCOMPATIBLE, "INCOMPATIBLE");
  }

  @Test
  public void status_REQUIRE_SONAR_UPGRADE_is_displayed_REQUIRES_SYSTEM_UPGRADE_in_JSON() throws Exception {
    loggedAsAdmin();
    checkStatusDisplayedInJson(REQUIRE_SONAR_UPGRADE, "REQUIRES_SYSTEM_UPGRADE");
  }

  @Test
  public void status_DEPENDENCIES_REQUIRE_SONAR_UPGRADE_is_displayed_DEPS_REQUIRE_SYSTEM_UPGRADE_in_JSON() throws Exception {
    loggedAsAdmin();
    checkStatusDisplayedInJson(DEPENDENCIES_REQUIRE_SONAR_UPGRADE, "DEPS_REQUIRE_SYSTEM_UPGRADE");
  }

  @Test
  public void fail_when_user_is_not_admin() throws Exception {
    userSession.login("user").setGlobalPermissions(DASHBOARD_SHARING);

    expectedException.expect(ForbiddenException.class);
    underTest.handle(request, response);
  }

  private void checkStatusDisplayedInJson(PluginUpdate.Status status, String expectedValue) throws Exception {
    when(updateCenter.findAvailablePlugins()).thenReturn(of(
      pluginUpdate(release(PLUGIN_1, "1.0.0"), status)
    ));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(
      "{" +
        "  \"plugins\": [" +
        "    {" +
        "      \"update\": {" +
        "        \"status\": \"" + expectedValue + "\"" +
        "      }" +
        "    }" +
        "  ]" +
        "}"
    );
  }

  private void loggedAsAdmin() {
    userSession.login("admin").setGlobalPermissions(SYSTEM_ADMIN);
  }

}
