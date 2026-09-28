from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.textinput import TextInput
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.core.window import Window

Window.clearcolor = (0.95, 0.95, 0.95, 1)

class SmartDocumentApp(App):
    def build(self):
        layout = BoxLayout(orientation='vertical', padding=15, spacing=12)
        
        self.title_label = Label(
            text="Smart Document Assistant",
            font_size='22sp',
            bold=True,
            color=(0.1, 0.1, 0.1, 1),
            size_hint_y=0.12
        )
        layout.add_widget(self.title_label)
        
        self.input_text = TextInput(
            hint_text="Enter text here to process...",
            multiline=True,
            size_hint_y=0.45,
            padding_x=10,
            padding_y=10
        )
        layout.add_widget(self.input_text)
        
        self.process_btn = Button(
            text="Process & Summarize",
            size_hint_y=0.13,
            background_color=(0.2, 0.5, 0.9, 1),
            color=(1, 1, 1, 1),
            bold=True
        )
        self.process_btn.bind(on_press=self.process_action)
        layout.add_widget(self.process_btn)
        
        self.output_label = Label(
            text="Result will appear here...",
            color=(0.2, 0.2, 0.2, 1),
            size_hint_y=0.3
        )
        layout.add_widget(self.output_label)
        
        return layout

    def process_action(self, instance):
        user_input = self.input_text.text.strip()
        if user_input:
            summary = user_input[:60] + "..." if len(user_input) > 60 else user_input
            self.output_label.text = f"Result Summary:\n{summary}"
        else:
            self.output_label.text = "Please enter some text first!"

if __name__ == '__main__':
    SmartDocumentApp().run()
