import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createDrawerNavigator } from '@react-navigation/drawer';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';

// Screens
import HomeScreen from '../screens/HomeScreen';
import ProxiesScreen from '../screens/ProxiesScreen';
import ProfilesScreen from '../screens/ProfilesScreen';
import ConnectionsScreen from '../screens/ConnectionsScreen';
import RulesScreen from '../screens/RulesScreen';
import LogsScreen from '../screens/LogsScreen';
import SettingsScreen from '../screens/SettingsScreen';
import ProfileEditorScreen from '../screens/ProfileEditorScreen';
import ProxyDetailScreen from '../screens/ProxyDetailScreen';

export type RootStackParamList = {
  Main: undefined;
  ProfileEditor: { profileId?: string };
  ProxyDetail: { proxyName: string };
};

export type MainTabParamList = {
  Home: undefined;
  Proxies: undefined;
  Profiles: undefined;
  Connections: undefined;
  Settings: undefined;
};

const Stack = createNativeStackNavigator<RootStackParamList>();
const Tab = createBottomTabNavigator<MainTabParamList>();
const Drawer = createDrawerNavigator();

function MainTabs() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        tabBarIcon: ({ focused, color, size }) => {
          let iconName: string;

          switch (route.name) {
            case 'Home':
              iconName = focused ? 'home' : 'home-outline';
              break;
            case 'Proxies':
              iconName = focused ? 'wifi' : 'wifi-off';
              break;
            case 'Profiles':
              iconName = focused ? 'file-document' : 'file-document-outline';
              break;
            case 'Connections':
              iconName = focused ? 'lan-connect' : 'lan-disconnect';
              break;
            case 'Settings':
              iconName = focused ? 'cog' : 'cog-outline';
              break;
            default:
              iconName = 'circle';
          }

          return <Icon name={iconName} size={size} color={color} />;
        },
        tabBarActiveTintColor: '#2196F3',
        tabBarInactiveTintColor: 'gray',
        headerShown: false,
      })}
    >
      <Tab.Screen 
        name="Home" 
        component={HomeScreen}
        options={{ title: '首页' }}
      />
      <Tab.Screen 
        name="Proxies" 
        component={ProxiesScreen}
        options={{ title: '代理' }}
      />
      <Tab.Screen 
        name="Profiles" 
        component={ProfilesScreen}
        options={{ title: '配置' }}
      />
      <Tab.Screen 
        name="Connections" 
        component={ConnectionsScreen}
        options={{ title: '连接' }}
      />
      <Tab.Screen 
        name="Settings" 
        component={SettingsScreen}
        options={{ title: '设置' }}
      />
    </Tab.Navigator>
  );
}

function DrawerNavigator() {
  return (
    <Drawer.Navigator
      screenOptions={{
        drawerActiveTintColor: '#2196F3',
        drawerInactiveTintColor: 'gray',
      }}
    >
      <Drawer.Screen 
        name="MainTabs" 
        component={MainTabs}
        options={{ 
          title: 'Clash Verge',
          drawerLabel: '主页'
        }}
      />
      <Drawer.Screen 
        name="Rules" 
        component={RulesScreen}
        options={{ title: '规则' }}
      />
      <Drawer.Screen 
        name="Logs" 
        component={LogsScreen}
        options={{ title: '日志' }}
      />
    </Drawer.Navigator>
  );
}

export function AppNavigator() {
  return (
    <Stack.Navigator>
      <Stack.Screen 
        name="Main" 
        component={DrawerNavigator}
        options={{ headerShown: false }}
      />
      <Stack.Screen 
        name="ProfileEditor" 
        component={ProfileEditorScreen}
        options={{ title: '编辑配置' }}
      />
      <Stack.Screen 
        name="ProxyDetail" 
        component={ProxyDetailScreen}
        options={{ title: '代理详情' }}
      />
    </Stack.Navigator>
  );
}

