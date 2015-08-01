#!/usr/bin/python3

# classes from Jerry:
from views.moves_edit_view import MovesEditView
from views.chessboard_view import ChessboardView
from controller import file_menu_ctr
from controller import edit_menu_ctr
from controller import game_menu_controller
from model.gamestate import GameState
from model.gamestate import MODE_ENTER_MOVES
from dialogs.dialog_about import DialogAbout
from uci.uci_controller import Uci_controller
from model.user_settings import UserSettings,InternalEngine
from model.database import Database
from model.model import Model
from controller.edit_menu_ctr import EditMenuController
from controller.file_menu_ctr import FileMenuController

# PyQt and python system functions / external libs
from  PyQt4.QtGui import *
from  PyQt4.QtCore import *
from functools import partial
import pickle
from util.appdirs import *


from main_window import MainWindow

def we_are_frozen():
    # All of the modules are built-in to the interpreter, e.g., by py2exe, py2app...
    return hasattr(sys, "frozen")

def module_path():
    encoding = sys.getfilesystemencoding()
    if we_are_frozen():
        return os.path.dirname(sys.executable)
    return os.path.dirname(__file__)



sys.setrecursionlimit(3000)

app = QApplication(sys.argv)

# get locale
#qm = 'qt_' + QLocale().name() + 's.qm'
lan = QLocale().name()[0:2]
qt_translation = "qt_"+lan+".qm"
jerry_translation = "jerry_"+lan+".qm"

#load qt localization
qt_translator = QTranslator(app)
qt_translator.load(qt_translation,"i18n/qm")
app.installTranslator(qt_translator)

#load jerry localization
translator = QTranslator(app)
translator.load(jerry_translation,"i18n/qm/")
app.installTranslator(translator)


main = MainWindow()

def about_to_quit():
    main.model.dump()


# set app icon
app_icon = QIcon()
app_icon.addFile('res/icons_taskbar/icon16.png', QSize(16,16))
app_icon.addFile('res/icons_taskbar/icon24.png', QSize(24,24))
app_icon.addFile('res/icons_taskbar/icon32.png', QSize(32,32))
app_icon.addFile('res/icons/taskbar/icon48.png', QSize(48,48))
app_icon.addFile('res/icons_taskbar/icon256.png',QSize(256,256))
app.setWindowIcon(app_icon)

app.setActiveWindow(main)
app.aboutToQuit.connect(about_to_quit) # myExitHandler is a callable
main.show()

#main.setFocus()
sys.exit(app.exec_())
