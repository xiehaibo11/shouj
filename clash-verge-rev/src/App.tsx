import React, { useEffect } from 'react';
import { StatusBar, useColorScheme } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { Provider as PaperProvider } from 'react-native-paper';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SWRConfig } from 'swr';

import { AppNavigator } from './navigation/AppNavigator';
import { lightTheme, darkTheme } from './theme';
import { initializeApp } from './services/initialize';
import { ErrorBoundary } from './components/ErrorBoundary';

function App(): React.JSX.Element {
  const colorScheme = useColorScheme();
  const theme = colorScheme === 'dark' ? darkTheme : lightTheme;

  useEffect(() => {
    // 初始化应用
    initializeApp().catch(console.error);
  }, []);

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
        <PaperProvider theme={theme}>
          <SWRConfig
            value={{
              errorRetryCount: 3,
              errorRetryInterval: 5000,
              dedupingInterval: 2000,
              focusThrottleInterval: 5000,
            }}
          >
            <ErrorBoundary>
              <NavigationContainer theme={theme}>
                <StatusBar
                  barStyle={
                    colorScheme === 'dark' ? 'light-content' : 'dark-content'
                  }
                  backgroundColor={theme.colors.background}
                />
                <AppNavigator />
              </NavigationContainer>
            </ErrorBoundary>
          </SWRConfig>
        </PaperProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}

export default App;

