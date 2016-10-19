package jmk.reddit.analogbot.object;

public enum CurrentState {
	new_post, 
	mod_alerted, 
	winner_requested, 
	winner_selected, 
	winner_notified,
	winner_responded,
	response_posted,
	error,
	error_mod_alerted };
