package one.june.leave_management.adapter.inbound.slack.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Slack view submission DTOs
 * Verifies JSON serialization and deserialization of Slack payloads
 */
class SlackViewSubmissionRequestTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeViewSubmissionRequest() throws Exception {
        // Given
        String json = """
                {
                    "type": "view_submission",
                    "team": {
                        "id": "T12345",
                        "domain": "example"
                    },
                    "user": {
                        "id": "U12345",
                        "username": "testuser",
                        "name": "Test User",
                        "team_id": "T12345"
                    },
                    "api_app_id": "A12345",
                    "token": "verification_token",
                    "trigger_id": "trigger123",
                    "view": {
                        "id": "V12345",
                        "team_id": "T12345",
                        "type": "modal",
                        "callback_id": "leave_application_submit",
                        "state": {
                            "values": {
                                "leave_type_category_block": {
                                    "leave_type_category_action": {
                                        "type": "radio_buttons",
                                        "selected_option": {
                                            "text": {
                                                "type": "plain_text",
                                                "text": "Annual Leave"
                                            },
                                            "value": "ANNUAL_LEAVE"
                                        }
                                    }
                                },
                                "leave_duration_block": {
                                    "leave_duration_action": {
                                        "type": "radio_buttons",
                                        "selected_option": {
                                            "text": {
                                                "type": "plain_text",
                                                "text": "Full Day"
                                            },
                                            "value": "FULL_DAY"
                                        }
                                    }
                                },
                                "start_date_block": {
                                    "start_date_action": {
                                        "type": "datepicker",
                                        "selected_date": "2024-01-15"
                                    }
                                },
                                "end_date_block": {
                                    "end_date_action": {
                                        "type": "datepicker",
                                        "selected_date": "2024-01-15"
                                    }
                                },
                                "reason_block": {
                                    "reason_action": {
                                        "type": "plain_text_input",
                                        "value": "Family vacation"
                                    }
                                }
                            }
                        },
                        "private_metadata": "U12345",
                        "title": {
                            "type": "plain_text",
                            "text": "Apply for Leave"
                        }
                    }
                }
                """;

        // When
        SlackViewSubmissionRequest request = objectMapper.readValue(json, SlackViewSubmissionRequest.class);

        // Then
        assertNotNull(request);
        assertEquals("view_submission", request.getType());
        assertEquals("T12345", request.getTeam().getId());
        assertEquals("U12345", request.getUser().getId());
        assertEquals("A12345", request.getApiAppId());
        assertEquals("trigger123", request.getTriggerId());

        // Verify view
        SlackViewSubmissionRequest.SlackView view = request.getView();
        assertNotNull(view);
        assertEquals("V12345", view.getId());
        assertEquals("leave_application_submit", view.getCallbackId());
        assertEquals("U12345", view.getPrivateMetadata());

        // Verify state values
        assertNotNull(view.getState());
        var stateValues = view.getState().getValues();

        // Verify leave type
        assertTrue(stateValues.containsKey("leave_type_category_block"));
        var leaveTypeAction = stateValues.get("leave_type_category_block").get("leave_type_category_action");
        assertNotNull(leaveTypeAction);
        assertEquals("radio_buttons", leaveTypeAction.getType());
        assertNotNull(leaveTypeAction.getSelectedOption());
        assertEquals("ANNUAL_LEAVE", leaveTypeAction.getSelectedOption().getValue());
        assertEquals("Annual Leave", leaveTypeAction.getSelectedOption().getText().getText());

        // Verify duration
        assertTrue(stateValues.containsKey("leave_duration_block"));
        var durationAction = stateValues.get("leave_duration_block").get("leave_duration_action");
        assertNotNull(durationAction);
        assertEquals("FULL_DAY", durationAction.getSelectedOption().getValue());

        // Verify start date
        assertTrue(stateValues.containsKey("start_date_block"));
        var startDateAction = stateValues.get("start_date_block").get("start_date_action");
        assertNotNull(startDateAction);
        assertEquals("2024-01-15", startDateAction.getSelectedDate());

        // Verify end date
        assertTrue(stateValues.containsKey("end_date_block"));
        var endDateAction = stateValues.get("end_date_block").get("end_date_action");
        assertNotNull(endDateAction);
        assertEquals("2024-01-15", endDateAction.getSelectedDate());

        // Verify reason
        assertTrue(stateValues.containsKey("reason_block"));
        var reasonAction = stateValues.get("reason_block").get("reason_action");
        assertNotNull(reasonAction);
        assertEquals("Family vacation", reasonAction.getValue());
    }

    @Test
    void shouldDeserializeViewSubmissionWithOptionalEndDateNull() throws Exception {
        // Given
        String json = """
                {
                    "type": "view_submission",
                    "team": {"id": "T12345", "domain": "example"},
                    "user": {"id": "U12345", "username": "testuser", "name": "Test User", "team_id": "T12345"},
                    "api_app_id": "A12345",
                    "token": "verification_token",
                    "trigger_id": "trigger123",
                    "view": {
                        "id": "V12345",
                        "team_id": "T12345",
                        "type": "modal",
                        "callback_id": "leave_application_submit",
                        "state": {
                            "values": {
                                "leave_type_category_block": {
                                    "leave_type_category_action": {
                                        "type": "radio_buttons",
                                        "selected_option": {
                                            "text": {"type": "plain_text", "text": "Annual Leave"},
                                            "value": "ANNUAL_LEAVE"
                                        }
                                    }
                                },
                                "leave_duration_block": {
                                    "leave_duration_action": {
                                        "type": "radio_buttons",
                                        "selected_option": {
                                            "text": {"type": "plain_text", "text": "Full Day"},
                                            "value": "FULL_DAY"
                                        }
                                    }
                                },
                                "start_date_block": {
                                    "start_date_action": {
                                        "type": "datepicker",
                                        "selected_date": "2024-01-15"
                                    }
                                }
                            }
                        },
                        "private_metadata": "U12345",
                        "title": {"type": "plain_text", "text": "Apply for Leave"}
                    }
                }
                """;

        // When
        SlackViewSubmissionRequest request = objectMapper.readValue(json, SlackViewSubmissionRequest.class);

        // Then
        assertNotNull(request);
        var stateValues = request.getView().getState().getValues();

        // End date block should not exist
        assertFalse(stateValues.containsKey("end_date_block"));

        // Reason block should not exist
        assertFalse(stateValues.containsKey("reason_block"));
    }

    @Test
    void shouldDeserializeOptionalHolidayLeaveType() throws Exception {
        // Given
        String json = """
                {
                    "type": "view_submission",
                    "team": {"id": "T12345", "domain": "example"},
                    "user": {"id": "U12345", "username": "testuser", "name": "Test User", "team_id": "T12345"},
                    "api_app_id": "A12345",
                    "token": "verification_token",
                    "trigger_id": "trigger123",
                    "view": {
                        "id": "V12345",
                        "team_id": "T12345",
                        "type": "modal",
                        "callback_id": "leave_application_submit",
                        "state": {
                            "values": {
                                "leave_type_category_block": {
                                    "leave_type_category_action": {
                                        "type": "radio_buttons",
                                        "selected_option": {
                                            "text": {"type": "plain_text", "text": "Optional Holiday"},
                                            "value": "OPTIONAL_HOLIDAY"
                                        }
                                    }
                                },
                                "leave_duration_block": {
                                    "leave_duration_action": {
                                        "type": "radio_buttons",
                                        "selected_option": {
                                            "text": {"type": "plain_text", "text": "First Half"},
                                            "value": "FIRST_HALF"
                                        }
                                    }
                                },
                                "start_date_block": {
                                    "start_date_action": {
                                        "type": "datepicker",
                                        "selected_date": "2024-02-10"
                                    }
                                }
                            }
                        },
                        "private_metadata": "U12345",
                        "title": {"type": "plain_text", "text": "Apply for Leave"}
                    }
                }
                """;

        // When
        SlackViewSubmissionRequest request = objectMapper.readValue(json, SlackViewSubmissionRequest.class);

        // Then
        assertNotNull(request);
        var stateValues = request.getView().getState().getValues();

        var leaveTypeAction = stateValues.get("leave_type_category_block").get("leave_type_category_action");
        assertEquals("OPTIONAL_HOLIDAY", leaveTypeAction.getSelectedOption().getValue());

        var durationAction = stateValues.get("leave_duration_block").get("leave_duration_action");
        assertEquals("FIRST_HALF", durationAction.getSelectedOption().getValue());
    }

    @Test
    void shouldHandleNullReason() throws Exception {
        // Given
        String json = """
                {
                    "type": "view_submission",
                    "team": {"id": "T12345", "domain": "example"},
                    "user": {"id": "U12345", "username": "testuser", "name": "Test User", "team_id": "T12345"},
                    "api_app_id": "A12345",
                    "token": "verification_token",
                    "trigger_id": "trigger123",
                    "view": {
                        "id": "V12345",
                        "team_id": "T12345",
                        "type": "modal",
                        "callback_id": "leave_application_submit",
                        "state": {
                            "values": {
                                "leave_type_category_block": {
                                    "leave_type_category_action": {
                                        "type": "radio_buttons",
                                        "selected_option": {
                                            "text": {"type": "plain_text", "text": "Annual Leave"},
                                            "value": "ANNUAL_LEAVE"
                                        }
                                    }
                                },
                                "leave_duration_block": {
                                    "leave_duration_action": {
                                        "type": "radio_buttons",
                                        "selected_option": {
                                            "text": {"type": "plain_text", "text": "Full Day"},
                                            "value": "FULL_DAY"
                                        }
                                    }
                                },
                                "start_date_block": {
                                    "start_date_action": {
                                        "type": "datepicker",
                                        "selected_date": "2024-01-15"
                                    }
                                },
                                "end_date_block": {
                                    "end_date_action": {
                                        "type": "datepicker",
                                        "selected_date": "2024-01-15"
                                    }
                                }
                            }
                        },
                        "private_metadata": "U12345",
                        "title": {"type": "plain_text", "text": "Apply for Leave"}
                    }
                }
                """;

        // When
        SlackViewSubmissionRequest request = objectMapper.readValue(json, SlackViewSubmissionRequest.class);

        // Then
        assertNotNull(request);
        var stateValues = request.getView().getState().getValues();

        // Reason block should not exist
        assertFalse(stateValues.containsKey("reason_block"));
    }
}
