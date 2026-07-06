import {
  Activity,
  BarChart3,
  BookOpen,
  ClipboardList,
  Database,
  FileClock,
  KeyRound,
  LayoutDashboard,
  ListChecks,
  RefreshCcw,
  ShieldCheck,
  Trophy,
  UserRoundCheck,
  Users,
} from 'lucide-react';
import type { IconKey } from '../types';

export const dashboardIcons = {
  activity: Activity,
  'bar-chart': BarChart3,
  'book-open': BookOpen,
  'clipboard-list': ClipboardList,
  database: Database,
  'file-clock': FileClock,
  'key-round': KeyRound,
  'layout-dashboard': LayoutDashboard,
  'list-checks': ListChecks,
  refresh: RefreshCcw,
  'shield-check': ShieldCheck,
  trophy: Trophy,
  'user-check': UserRoundCheck,
  users: Users,
} satisfies Record<IconKey, typeof Activity>;
