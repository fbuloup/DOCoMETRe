package fr.univamu.ism.docometre.editors;

import org.eclipse.jface.text.templates.Template;

class CustomerFunctionTemplate extends Template {
		
		private int level;
		
		public CustomerFunctionTemplate(String name, String description, String pattern, int level) {
			super(name, description, CustomerFunctionCompletionProcessor.KEY, pattern, true);
			this.level = level;
		}
		
		public CustomerFunctionTemplate(String name, String description, int level) {
			super(name, description, CustomerFunctionCompletionProcessor.KEY, name + " = ", true);
			this.level = level;
		}

		@Override
		public boolean matches(String prefix, String contextTypeId) {
			boolean match = super.matches(prefix, contextTypeId);
			return match && getPattern().startsWith(prefix);
		}
		
		public int getLevel() {
			return level;
		}
		
	}