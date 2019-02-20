package example.scenario.groovy.types

def cmd1 = "echo Hello world!".execute()
def cmd2 = "ps alx | grep java".execute()

cmd1.waitFor()
cmd2.waitFor()
